/*
 * Copyright 2015-2016 Alessio Vinerbi. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.anchorage.system;

import java.io.File;
import java.nio.file.SecureDirectoryStream;
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

import com.anchorage.docks.containers.subcontainers.DockSplitterContainer;
import com.anchorage.docks.containers.subcontainers.DockTabberContainer;
import com.anchorage.docks.node.DockNode;
import com.anchorage.docks.node.DockNode.DockPosition;
import com.anchorage.docks.node.ui.DockUIPanel;
import com.anchorage.docks.stations.DockStation;
import com.anchorage.docks.stations.DockSubStation;
import com.sun.javafx.css.StyleManager;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;

/**
 *
 * @author Alessio
 */
public class AnchorageSystem {

    private static final List<DockStation> stations;
    private static final Image emptyIconImage;
    private static final Image emptySubstationIconImage;

    private static DockStation currentStationFromDrag;

    static {
        stations = new ArrayList<>();
        emptyIconImage = new Image("empty.png");
        emptySubstationIconImage = new Image("substation.png");
    }

    public static DockStation createStation() {
        DockStation station = new DockStation();
        stations.add(station);
        return station;
    }

    public static DockStation createCommonStation() {
        DockStation station = new DockStation(true);
        stations.add(station);
        return station;
    }

    public static DockSubStation createSubStation(DockStation parentStation, String title) {
        DockSubStation station = new DockSubStation(new DockUIPanel(title, new DockStation(), true, emptySubstationIconImage));
        return station;
    }

    public static DockNode createDock(String title, Node content) {
        return createDock(title, content, emptyIconImage);
    }

    public static DockNode createDock(String title, Node content, Image icon) {
        DockUIPanel panel = new DockUIPanel(title, content, false, icon);
        DockNode container = new DockNode(panel);
        return container;
    }

    public static void installDefaultStyle() {
        StyleManager.getInstance()
                .addUserAgentStylesheet("AnchorFX.css");
    }

    public static void prepareDraggingZoneFor(DockStation station, DockNode source) {
        currentStationFromDrag = station;
        station.prepareZones(source);
        if (currentStationFromDrag.isSubStation())
            return;
        if (station.isCommonStation()) {
            stations.stream().filter(s -> s != station && s.isCommonStation()).forEach(s -> s.prepareZones(source));
        }
    }

    public static void searchTargetNode(double x, double y) {
        
        if (currentStationFromDrag.isCommonStation() && !currentStationFromDrag.isSubStation()) {
            stations.stream().filter(s -> s.isCommonStation()).forEach(s -> s.searchTargetNode(x, y));
        } else {
            currentStationFromDrag.searchTargetNode(x, y);
        }
    }

    public static void finalizeDragging() {
        if (currentStationFromDrag.isSubStation()) {
            currentStationFromDrag.closeZones();
            currentStationFromDrag.finalizeDrag();
        } else {
            if (currentStationFromDrag.isCommonStation())
                stations.stream().filter(s -> s.isCommonStation()).forEach(s -> s.closeZones());
            else
                currentStationFromDrag.closeZones();
            
            DockStation selectedStation = stations.stream().filter(s -> s.isSelected()).findFirst().orElse(null);
            if (selectedStation != null && currentStationFromDrag.isCommonStation()) {
                selectedStation.finalizeDrag();
            } else {
                currentStationFromDrag.finalizeDrag();
            }
        }

    }
    
    /////////////////////////////
    // Save Layout
    ////////////////////////////
    
    /**
     * Saves layout of the DockStation.
     * <br/><b>Warning:</b> Does not work with multiple instances of one UI class. 
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
            parseStationTree(rootNode, doc, mainRootElement, 0);

            System.out.println("XML DOM created successfully");
            
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
    
    private static void parseStationTree(final Parent parent, final Document doc, final Element element, final int index) {
        if (!parent.getChildrenUnmodifiable().isEmpty()) {
            for (Node child: parent.getChildrenUnmodifiable()) {
            	
            	final boolean allowedClasses = child instanceof DockSplitterContainer || child instanceof DockNode || child instanceof DockTabberContainer || /*child instanceof DockStation ||*/ child instanceof DockSubStation;
            	Element childElement;
            	
            	// Omit elements, that are not necessary.
            	if (allowedClasses) {
            	    
                	// create and append child element
            		final String elementName = child.getClass().getSimpleName().trim().replaceAll("/[^A-Za-z]/", "");
                	childElement = doc.createElement(elementName);
                	
                	switch (elementName) {
					case "DockSplitterContainer":
							DockSplitterContainer container = (DockSplitterContainer) child;
							String dividerpositions = "";
							for (double position : container.getDividerPositions()) {
								dividerpositions += position + " ";
							}
							childElement.setAttribute("dividerPositions", dividerpositions);
							childElement.setAttribute("orientation", container.getOrientation().toString());
						break;
					case "DockNode":
					    DockUIPanel dockUIPanel = ((DockNode) child).getContent();
					    childElement.setAttribute("name", dockUIPanel.titleProperty().get());
					    childElement.setAttribute("index", Integer.toString(index));
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
                	Element uiElement = doc.createElement(((DockUIPanel)child).getNodeContent().getClass().getSimpleName().trim().replaceAll("/[^A-Za-z]/", ""));
                	childElement.appendChild(uiElement);
                } else if (child instanceof Parent) {
                    final int newLevel = index + 1;
                    parseStationTree((Parent)child, doc, childElement, newLevel);
                }
              
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
	            
	            NodeList list = doc.getElementsByTagName("DockNode");
	            
	            // Return if no elements in station
	            if (list.getLength() == 0) {
	            	return false;
	            }
	            
	            org.w3c.dom.Node startNode = list.item(0);
	            
	            for (int i = 1; i < list.getLength(); i++) {
	            	int currentIndex = Integer.parseInt(startNode.getAttributes().getNamedItem("index").getNodeValue());
	            	int newIndex = Integer.parseInt(list.item(i).getAttributes().getNamedItem("index").getNodeValue());
	
	            	if (newIndex > currentIndex) {
	            		startNode = list.item(i);
	            	}
	            }
	            System.out.println("############ StartNode");
	            System.out.println(startNode.getNodeName());
	            System.out.println(startNode.getAttributes().getNamedItem("index").getNodeValue());
	            System.out.println(startNode.getAttributes().getNamedItem("name").getNodeValue());
	            System.out.println("############ StartNode End");
	            System.out.println();
	            
	            // Get all already docked DockNodes and remove all already docked nodes
	            final List<DockNode> dockNodeList = new ArrayList<DockNode>(dockStation.getDockNodes());
	            for (DockNode dockNode : dockNodeList) {
	            	dockNode.undock();
	//            	System.out.println(dockNode.getContent().titleProperty().get());
	            }
	//            System.out.println();
	//            System.out.println();
	            
	            handleNode(dockStation, dockNodeList, startNode.getParentNode());
	            
	         } catch (Exception e) {
	            e.printStackTrace();
	            return false;
	         }
	    	return true;
    }
    
    private static void handleNode(final DockStation station, final List<DockNode> dockNodeList, final org.w3c.dom.Node parentNode) {
	    	System.out.println("handleNode() - " + parentNode.getNodeName());
	    	
	    	switch (parentNode.getNodeName()) {
	    	case "DockSplitterContainer":
	    		handleSplitterContainer(station, dockNodeList, parentNode);
	    		break;
	    	case "DockTabberContainer":
	    		handleTabberContainer(station, dockNodeList, parentNode);
	    		break;
	    	case "DockSubStation":
	//    		handleSubStation(station, list.item(i));
	    		break;
	    	default:
	    		System.out.println("default");
	    		break;
	    	}
    }
    
    private static void handleSplitterContainer(final DockStation station, final List<DockNode> dockNodeList, org.w3c.dom.Node parentNode) {    	
	    	final double dividerPosition = Double.parseDouble(parentNode.getAttributes().getNamedItem("dividerPositions").getNodeValue());
	    	final String orientation = parentNode.getAttributes().getNamedItem("orientation").getNodeValue();
	    	final List<org.w3c.dom.Node> childNodes = getChildElements(parentNode.getChildNodes());
	
	    	org.w3c.dom.Node firstNode = null;
	    	org.w3c.dom.Node secondNode = null;
	
	    	for (org.w3c.dom.Node childNode : childNodes) {
	    		if (firstNode == null) {
	    			firstNode = childNode;
	    		} else if (secondNode == null) {
	    			secondNode = childNode;
	    		}
	    	}
	
	    	// Return if there are not two nodes.
	    	if (firstNode == null || secondNode == null) {
	    		return;
	    	}
	
	    	DockNode firstDockNode = null;
	    	DockNode secondDockNode = null;
	
	    	for (DockNode node : dockNodeList) {
	    		if (firstNode.getAttributes().getNamedItem("name") != null && node.getContent().titleProperty().getValue().equals(firstNode.getAttributes().getNamedItem("name").getNodeValue())) {
	    			System.out.println("found first");
	    			firstDockNode = node;
	    		} else if (secondNode.getAttributes().getNamedItem("name") != null && node.getContent().titleProperty().getValue().equals(secondNode.getAttributes().getNamedItem("name").getNodeValue())) {
	    			System.out.println("found second");
	    			secondDockNode = node;
	    		}
	    	}
	
	    	if (firstNode.getNodeName().equals("DockNode") && secondNode.getNodeName().equals("DockNode")) {
	    		firstDockNode.dock(station, DockPosition.CENTER);
	
	    		if (orientation.equals("HORIZONTAL")) {
	    			secondDockNode.dock(firstDockNode, DockPosition.RIGHT, dividerPosition);
	    		} else {
	    			secondDockNode.dock(firstDockNode, DockPosition.BOTTOM, dividerPosition);
	    		}
	    	} else {
	    		// has one element been docked already? then just execute docking of the other node.
	    		if (firstNode.getAttributes().getNamedItem("docked") != null) { // dock second node right or bottom
	    			System.out.println("firstNode already docked");
	
	    			if (secondDockNode != null) {
		    			if (orientation.equals("HORIZONTAL")) {
		    				secondDockNode.dock(station, DockPosition.RIGHT, dividerPosition);
		    			} else {
		    				secondDockNode.dock(station, DockPosition.BOTTOM, dividerPosition);
		    			}
	    			} else {
		    			handleNode(station, dockNodeList, secondNode);
	    			}
	    		} else if (secondNode.getAttributes().getNamedItem("docked") != null) {
	    			System.out.println("secondNode already docked");
	    			
	    			if (firstDockNode != null) {
		    			if (orientation.equals("HORIZONTAL")) {
		    				firstDockNode.dock(station, DockPosition.LEFT, dividerPosition);
		    			} else {
		    				firstDockNode.dock(station, DockPosition.TOP, dividerPosition);
		    			}
	    			} else {
	    				handleNode(station, dockNodeList, firstNode);
	    			}
	    		}
	    	}
	
	    	// Mark parent element, that children have been docked.
	    	((Element)parentNode).setAttribute("docked", "true");
	    	handleNode(station, dockNodeList, parentNode.getParentNode());
    }
    
    private static void handleTabberContainer(final DockStation station, final List<DockNode> dockNodeList, org.w3c.dom.Node parentNode) {
        final List<org.w3c.dom.Node> childNodes = getChildElements(parentNode.getChildNodes());
        
        // Return if there are not two nodes.
        if (childNodes.size() < 1) {
        		return;
        }

        List <DockNode> filteredDockNodeList = new ArrayList<>();
        for (org.w3c.dom.Node childNode : childNodes) {
        		if (!childNode.getNodeName().equals("DockNode")) {
        			return; // Something is wrong, return.
        		}
        		for (DockNode node : dockNodeList) {
        			if (childNode.getAttributes().getNamedItem("name") != null && node.getContent().titleProperty().getValue().equals(childNode.getAttributes().getNamedItem("name").getNodeValue())) {
        				filteredDockNodeList.add(node);
        			}
        		}
        }

        DockPosition dockPosition = DockPosition.CENTER;
        double dividerPosition = 0.5;
        
        if (parentNode.getParentNode().getNodeName().equals("DockSplitterContainer")) {
	        dividerPosition = Double.parseDouble(parentNode.getParentNode().getAttributes().getNamedItem("dividerPositions").getNodeValue());
	    		final String orientation = parentNode.getParentNode().getAttributes().getNamedItem("orientation").getNodeValue();	    		
	        
	        final List<org.w3c.dom.Node> parentNodes = getChildElements(parentNode.getParentNode().getChildNodes());
	        int childnumber = 0;
	        
	        for (int i = 0; i < parentNodes.size(); i++) {
	        		if (parentNodes.get(i).getNodeName().equals("DockTabberContainer") && parentNodes.get(i).getAttributes().getNamedItem("docked") == null) {
	        			childnumber = i;
	        		}
	        }
	        
	        if (orientation.equals("HORIZONTAL")) {
	        		if (childnumber == 0) {
	        			dockPosition = DockPosition.LEFT;
		        } else {
		        		dockPosition = DockPosition.RIGHT;
		        }
	    		} else {
	    			if (childnumber == 0) {
	        			dockPosition = DockPosition.TOP;
		        } else {
		        		dockPosition = DockPosition.BOTTOM;
		        }
	    		}	        
        }
        
        System.out.println("dividerPosition: " + dividerPosition);
		System.out.println("dockPosition: " + dockPosition.name());
        
        filteredDockNodeList.get(filteredDockNodeList.size() - 1).dock(station, dockPosition);
        for (int i = filteredDockNodeList.size() - 2; i > -1; i--) {
        		filteredDockNodeList.get(i).dock(filteredDockNodeList.get(filteredDockNodeList.size() - 1), DockPosition.CENTER);
        }
    }
    
    private static void handleSubStation(DockStation station, org.w3c.dom.Node node) {
        
    }
    
    private static List<org.w3c.dom.Node> getChildElements(final NodeList list) {
	    	List<org.w3c.dom.Node> returnList = new ArrayList<>();
	    	for (int i = 0; i < list.getLength(); i++) {
	    		if (list.item(i).getNodeName().equals("#text")) {
	    			continue;
	    		}
	    		returnList.add(list.item(i));
	    	}
	    	return returnList;
    }
}
