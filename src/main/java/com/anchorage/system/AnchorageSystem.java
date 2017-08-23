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

import com.anchorage.docks.node.DockNode;
import com.anchorage.docks.node.ui.DockUIPanel;
import com.anchorage.docks.stations.DockStation;
import com.anchorage.docks.stations.DockSubStation;
import com.sun.javafx.css.StyleManager;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.control.Control;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

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
    
    /**
     * Saves layout of the DockStation.
     * 
     * @param rootDockStation Root dock station
     * @return boolean if saving layout has been successful.
     */
    public static boolean saveLayout(DockStation rootDockStation) {
        if (rootDockStation.getChildren().isEmpty()) {
            return false;
        }
        
        final Parent rootNode = rootDockStation;
//        System.out.println(currentNode.getClass().getName());
        
        DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder icBuilder;
        
        try {
            icBuilder = icFactory.newDocumentBuilder();
            Document doc = icBuilder.newDocument();
            Element mainRootElement = doc.createElement(rootNode.getClass().getSimpleName().trim());
            doc.appendChild(mainRootElement);
            visit(rootNode, doc, mainRootElement);

            System.out.println("\nXML DOM Created Successfully..");
            System.out.println();
            
            // output DOM XML to console 
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
            DOMSource source = new DOMSource(doc);
            StreamResult console = new StreamResult(System.out);
            transformer.transform(source, console);
 
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return true;
    }
    
        
    private static void visit(final Parent parent, final Document doc, final Element element) {
        if (!parent.getChildrenUnmodifiable().isEmpty()) {
            for (Node child: parent.getChildrenUnmodifiable()) {
                
            	// create and append child element
            	System.out.println("child: " + child.getClass().getSimpleName().trim().replaceAll("/[^A-Za-z]/", ""));
//            	Element childElement = doc.createElement(child.getClass().getTypeName().trim());
            	if (child.getClass().getSimpleName().trim().replaceAll("/[^A-Za-z]/", "").isEmpty()) {
            		System.out.println("empty");
            		return;
            	}
            	Element childElement = doc.createElement(child.getClass().getSimpleName().trim().replaceAll("/[^A-Za-z]/", ""));
            	
//                childElement.setAttribute("id", id);
//                childElement.appendChild(getCompanyElements(doc, childElement, "Name", name));
//                childElement.appendChild(getCompanyElements(doc, childElement, "Type", age));
//                childElement.appendChild(getCompanyElements(doc, childElement, "Employees", role));
            	element.appendChild(childElement);

                if (child instanceof DockUIPanel) {
                	Element uiElement = doc.createElement(((DockUIPanel)child).getNodeContent().getClass().getSimpleName().trim().replaceAll("/[^A-Za-z]/", ""));
                	childElement.appendChild(uiElement);
                } else if (child instanceof Parent) {
                    visit((Parent)child, doc, childElement);
                }
              
            }
        }
    }
}
