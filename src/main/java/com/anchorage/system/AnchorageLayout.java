package com.anchorage.system;

import static com.anchorage.docks.containers.common.AnchorageSettings.FLOATING_NODE_DROPSHADOW_RADIUS;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.anchorage.docks.containers.StageFloatable;
import com.anchorage.docks.containers.common.DockCommons;
import com.anchorage.docks.containers.interfaces.DockContainer;
import com.anchorage.docks.containers.subcontainers.DockSplitterContainer;
import com.anchorage.docks.containers.subcontainers.DockTabberContainer;
import com.anchorage.docks.node.DockNode;
import com.anchorage.docks.node.ui.DockUIPanel;
import com.anchorage.docks.stations.DockStation;
import com.anchorage.docks.stations.DockSubStation;
import com.sun.javafx.robot.impl.FXRobotHelper;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.stage.Stage;

/**
 * @author Markus Traber
 *
 */
public class AnchorageLayout {
	
	
	/////////////////////////////
	// Save Layout
	////////////////////////////

	/**
	 * Saves layout of the DockStation. <br/>
	 * 
	 * @param rootDockStation Root dock station
	 * @param filePath file path where to save layout.
	 * @return boolean if saving layout has been successful.
	 */
	public static boolean saveLayout(DockStation rootDockStation, final String filePath) {
		if (rootDockStation.getChildren().isEmpty()) {
			return false;
		}

		final Parent rootNode = rootDockStation;

		DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder icBuilder;

		try {
			icBuilder = icFactory.newDocumentBuilder();
			Document doc = icBuilder.newDocument();
			Element mainRootElement = doc.createElement(rootNode.getClass().getSimpleName().trim());
			doc.appendChild(mainRootElement);
			
			// DockNodes, docked to station
			parseStationTree(rootNode, doc, mainRootElement);
			
			// floating DockNodes
			parseFloatingNodes(doc, mainRootElement);

			// DOM XML output
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filePath));
			transformer.transform(source, result);

			// console output
			// StreamResult console = new StreamResult(System.out);
			// transformer.transform(source, console);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	private static void parseStationTree(final Parent parent, final Document doc, final Element element) {
		if (!parent.getChildrenUnmodifiable().isEmpty()) {
			for (Node child : parent.getChildrenUnmodifiable()) {

				final boolean allowedClasses = child instanceof DockSplitterContainer || child instanceof DockNode || child instanceof DockTabberContainer || child instanceof DockSubStation;
				Element childElement;

				// Omit elements, that are not necessary.
				if (allowedClasses) {

					// create and append child element
					final String elementName = child.getClass().getSimpleName().trim().replaceAll("/[^A-Za-z]/", "");
					childElement = doc.createElement(elementName);

					switch (elementName) {
					case "DockSplitterContainer":
						DockSplitterContainer container = (DockSplitterContainer) child;
						String dividerpositions = Double.toString(container.getDividerPositions()[0]);
						childElement.setAttribute("dividerPositions", dividerpositions);
						childElement.setAttribute("orientation", container.getOrientation().toString());
						break;
					case "DockNode":
					case "DockSubStation":
						DockUIPanel dockUIPanel = ((DockNode) child).getContent();
						childElement.setAttribute("name", dockUIPanel.titleProperty().get());
						break;
					default:
						break;
					}
					element.appendChild(childElement);
				} else {
					childElement = element;
				}

				// Append UI element or dig deeper
				if (child instanceof DockUIPanel && !(parent instanceof DockSubStation)) {
					Element uiElement = doc.createElement(((DockUIPanel) child).getNodeContent().getClass().getSimpleName().trim().replaceAll("/[^A-Za-z]/", ""));
					childElement.appendChild(uiElement);
				} else if (child instanceof Parent) {
					parseStationTree((Parent) child, doc, childElement);
				}

			}
		}
	}
	
	private static void parseFloatingNodes(Document doc, Element mainRootElement) {
		Element floatingElement = doc.createElement("floating");
		mainRootElement.appendChild(floatingElement);
		for (Stage stage : FXRobotHelper.getStages()) {
			if (stage instanceof StageFloatable) {
				StageFloatable stageFloatable = (StageFloatable) stage;
				
				// create child element
				final String elementName = stageFloatable.getDockNode().getClass().getSimpleName().trim().replaceAll("/[^A-Za-z]/", "");
				Element childElement = doc.createElement(elementName);
				
				// name
				DockUIPanel dockUIPanel = stageFloatable.getDockNode().getContent();
				childElement.setAttribute("name", dockUIPanel.titleProperty().get());
				
				// position and size
				childElement.setAttribute("position-x", Double.toString(stageFloatable.getX() + FLOATING_NODE_DROPSHADOW_RADIUS));
				childElement.setAttribute("position-y", Double.toString(stageFloatable.getY() + FLOATING_NODE_DROPSHADOW_RADIUS));
				childElement.setAttribute("width", Double.toString(stageFloatable.getScene().getWidth()));
				childElement.setAttribute("height", Double.toString(stageFloatable.getScene().getHeight()));

				floatingElement.appendChild(childElement);
			}
		}
	}

	/////////////////////////////
	// Restore Layout
	////////////////////////////
	
	/**
	 * Restores layout of already docked nodes.
	 * 
	 * @param dockStation DockStation with all docked nodes.
	 * @param filePath Path to file, where layout is saved.
	 * @return boolean if restoring layout has been successful.
	 */
	public static boolean restoreLayout(final DockStation dockStation, final String filePath) {
		try {
			File inputFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			
			// Get all already docked DockNodes and remove all already docked nodes
			final List<DockNode> dockNodeList = getAndUndockAllDocks(dockStation.getDockNodes());
			
			List<org.w3c.dom.Node> firstChild = removeTextNodes(doc.getDocumentElement().getChildNodes());
			if (firstChild.isEmpty()) {
				return false; // no child elements
			}
			
			org.w3c.dom.Node startNode = firstChild.get(0);
			
			Node outerMostNode = handleNode(startNode, dockNodeList, dockStation);
			dockStation.getChildren().add(outerMostNode);
			
			// floating nodes
			if (firstChild.size() > 1) {
				org.w3c.dom.Node floating = firstChild.get(1);
				handleFloatingNodes(floating, dockNodeList, dockStation);
			}
			
			// Set parent containers (important for docking to function)
			setAllParentContainers(dockStation);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

    private static List<DockNode> getAndUndockAllDocks(final List<DockNode> dockNodeList) {
        final List<DockNode> list = new ArrayList<>(dockNodeList);
        final List<DockNode> returnList = new ArrayList<>(dockNodeList);
        
        for (DockNode dockNode : list) {
	        	dockNode.undock();
	        	
	        	if (dockNode instanceof DockSubStation) {
	        	    DockSubStation subStation = (DockSubStation) dockNode;
	        	    returnList.addAll(getAndUndockAllDocks(subStation.getSubStation().getDockNodes()));
	        	}
        }
        return returnList;
    }
	
	private static void setAllParentContainers(Node node) {
	    if (node instanceof DockContainer) {
	        DockContainer dockContainer = (DockContainer) node;
	        
	        List<Node> list = new ArrayList<>();
	        
	        if (dockContainer instanceof DockSplitterContainer) {
	            list.addAll(((DockSplitterContainer) dockContainer).getItems());
	        } else if (dockContainer instanceof DockTabberContainer) {
	            for (Tab tab : ((DockTabberContainer) dockContainer).getTabs()) {
	                list.add(tab.getContent());
	            }
	        } else if (dockContainer instanceof DockStation) {
	        		list.addAll(((DockStation) dockContainer).getChildren());
	        }
	        
	        for (Node childNode : list) {
	            if (childNode instanceof DockSubStation) {
	                DockSubStation subStation = (DockSubStation) childNode;
	                subStation.setParentContainer(dockContainer);
	                
	                if (!subStation.getSubStation().getChildren().isEmpty()) {
	                    setAllParentContainers(subStation.getSubStation());
	                }
	            } else if (childNode instanceof DockNode) {
	                // Set DockNode parent container
	                DockNode childDockNode = (DockNode) childNode;
	                childDockNode.setParentContainer(dockContainer);
	            } else if (childNode instanceof DockContainer) {
	                // Set container parent container and call method for it
	                DockContainer childDockContainer = (DockContainer) childNode;
	                childDockContainer.setParentContainer(dockContainer);
                    setAllParentContainers(childNode);
	            }
	        }
	    }
	}
	
	private static List<org.w3c.dom.Node> removeTextNodes(final NodeList list) {
	    	List<org.w3c.dom.Node> returnList = new ArrayList<>();
	    	for (int i = 0; i < list.getLength(); i++) {
	    		if (list.item(i).getNodeName().equals("#text")) {
	    			continue;
	    		}
	    		returnList.add(list.item(i));
	    	}
	    	return returnList;
    }
	
	private static void handleFloatingNodes(final org.w3c.dom.Node floating, final List<DockNode> dockNodeList, final DockStation dockStation) {
		List<org.w3c.dom.Node> floatingList = removeTextNodes(floating.getChildNodes());
		for (org.w3c.dom.Node node : floatingList) {	
			final double posX = Double.parseDouble(node.getAttributes().getNamedItem("position-x").getNodeValue());
			final double posY = Double.parseDouble(node.getAttributes().getNamedItem("position-y").getNodeValue());
			final double width = Double.parseDouble(node.getAttributes().getNamedItem("width").getNodeValue());
			final double height = Double.parseDouble(node.getAttributes().getNamedItem("height").getNodeValue());
			
			for (DockNode dockNode : dockNodeList) {
		        	if (node.getAttributes().getNamedItem("name") != null && dockNode.getContent().titleProperty().getValue().equals(node.getAttributes().getNamedItem("name").getNodeValue())) {
		        		dockNode.dockAsFloating(dockStation.getStationWindow(), dockStation, posX, posY, width, height);
		        	} 
	        }
		}
	}

    private static Node handleNode(final org.w3c.dom.Node node, final List<DockNode> dockNodeList, final DockStation dockStation) {
		switch (node.getNodeName()) {
		case "DockSplitterContainer":
			return handleDockSplitterContainer(node, dockNodeList, dockStation);
		case "DockTabberContainer":
			return handleDockTabberContainer(node, dockNodeList, dockStation);
		case "DockSubStation":
			return handleDockSubStation(node, dockNodeList, dockStation);
		case "DockNode": 
		    return handleDockNode(node, dockNodeList, dockStation);
		default:
			break;
		}
		return null;
	}

    private static Node handleDockSubStation(final org.w3c.dom.Node node, final List<DockNode> dockNodeList, final DockStation dockStation) {
        final List<org.w3c.dom.Node> childNodes = removeTextNodes(node.getChildNodes());
        final String name = node.getAttributes().getNamedItem("name").getNodeValue();
        DockSubStation subStation = AnchorageSystem.createSubStation(dockStation, name);
        subStation.stationProperty().set(dockStation);
        dockStation.add(subStation);
        
        if (childNodes.isEmpty()) {
            return subStation;
        }
        
        // A SubStation can only contain one following child element
        subStation.getSubStation().getChildren().add(handleNode(childNodes.get(0), dockNodeList, subStation.getSubStation()));
        
        return subStation;
    }

    private static Node handleDockNode(final org.w3c.dom.Node node, final List<DockNode> dockNodeList, final DockStation dockStation) {
        for (DockNode dockNode : dockNodeList) {
	        	if (node.getAttributes().getNamedItem("name") != null && dockNode.getContent().titleProperty().getValue().equals(node.getAttributes().getNamedItem("name").getNodeValue())) {
	        		dockStation.add(dockNode);
	        		dockNode.stationProperty().set(dockStation);
	        	    return dockNode;
	        	} 
        }
        return null;
    }
	
	private static Node handleDockSplitterContainer(final org.w3c.dom.Node node, final List<DockNode> dockNodeList, final DockStation dockStation) {
		final double dividerPosition = Double.parseDouble(node.getAttributes().getNamedItem("dividerPositions").getNodeValue());
		final List<org.w3c.dom.Node> childNodes = removeTextNodes(node.getChildNodes());
		final Orientation orientation;
		
		if (node.getAttributes().getNamedItem("orientation").getNodeValue().equals("VERTICAL")) {
			orientation = Orientation.VERTICAL;
		} else {
			orientation = Orientation.HORIZONTAL;
		}
		
		Node firstNode = handleNode(childNodes.get(0), dockNodeList, dockStation);
		Node secondNode = handleNode(childNodes.get(1), dockNodeList, dockStation);
				
		return DockCommons.createSplitter(firstNode, secondNode, orientation, dividerPosition);
	}
	
	private static Node handleDockTabberContainer(final org.w3c.dom.Node node, final List<DockNode> dockNodeList, final DockStation dockStation) {
		final List<org.w3c.dom.Node> childNodes = removeTextNodes(node.getChildNodes());
		
		List<Node> nodeList = new ArrayList<>();
		for (int i = childNodes.size() - 1; i >= 0; i--) {
			nodeList.add(handleNode(childNodes.get(i), dockNodeList, dockStation));
		}
		
		return DockCommons.createTabber(nodeList);
	}
}
