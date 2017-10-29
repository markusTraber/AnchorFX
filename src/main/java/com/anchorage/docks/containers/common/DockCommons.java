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

package com.anchorage.docks.containers.common;

import java.util.ArrayList;
import java.util.List;

import com.anchorage.docks.containers.interfaces.DockContainableComponent;
import com.anchorage.docks.containers.subcontainers.DockSplitterContainer;
import com.anchorage.docks.containers.subcontainers.DockTabberContainer;
import com.anchorage.docks.node.DockNode;
import com.anchorage.docks.node.DockNode.DockPosition;

import javafx.geometry.Orientation;
import javafx.scene.Node;

/**
 *
 * @author Alessio
 */
public class DockCommons {

    public static boolean isABorderPosition(DockNode.DockPosition position) {
        return position != DockNode.DockPosition.CENTER;
    }
    
    public static DockSplitterContainer createSplitter(Node firstNode, Node secondNode, Orientation orientation, double percentage) {
    		return createSplitter(firstNode, secondNode, orientation.equals(Orientation.VERTICAL) ? DockPosition.BOTTOM : DockPosition.RIGHT, percentage);
    }

    public static DockSplitterContainer createSplitter(Node existNode, Node newNode, DockNode.DockPosition position, double percentage) {
        DockSplitterContainer splitter = createEmptySplitter();
 
        if (position == DockNode.DockPosition.BOTTOM || position == DockNode.DockPosition.TOP) {
            splitter.setOrientation(Orientation.VERTICAL);
        }

        DockContainableComponent existContainableComponent = (DockContainableComponent) existNode;
        DockContainableComponent newContainableComponent = (DockContainableComponent) newNode;

        existContainableComponent.setParentContainer(splitter);
        newContainableComponent.setParentContainer(splitter);

        if (position == DockNode.DockPosition.BOTTOM || position == DockNode.DockPosition.RIGHT) {
            splitter.getItems().addAll(existNode, newNode);
        }
        else {
            splitter.getItems().addAll(newNode, existNode);
        }

        splitter.getStyleClass().add("docknode-split-pane");
        splitter.setDividerPositions(percentage);
        return splitter;
    }

    public static DockSplitterContainer createEmptySplitter() {
        return new DockSplitterContainer();
    }

    public static DockTabberContainer createTabber(Node existNode, Node newNode, DockNode.DockPosition position) {
    		List<Node> nodes = new ArrayList<>();
    		nodes.add(existNode);
    		nodes.add(newNode);
    		
    		return createTabber(nodes);
    }
    
    public static DockTabberContainer createTabber(List<Node> nodes) {
    		DockTabberContainer tabber = new DockTabberContainer();
    		tabber.getStyleClass().add("docknode-tab-pane");
    		for (Node node : nodes) {
    			if (node instanceof DockNode) {
    				DockNode newNode = (DockNode) node;
    				tabber.addAsTab(newNode);
    				newNode.ensureVisibility();
    			}
    		}
    		if (tabber.getTabs().isEmpty()) {
    			return null;
    		}
        return tabber;
    }
}
