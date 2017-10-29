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
package com.anchorage.docks.containers;

import static com.anchorage.docks.containers.common.AnchorageSettings.FLOATING_NODE_DROPSHADOW_RADIUS;

import com.anchorage.docks.node.DockNode;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class NodeDraggingPreview extends Stage {

  private DockNode node;
  private StackPane transparentRootPanel;
  private StackPane stackPanelContainer;
  private Window owner;

  public NodeDraggingPreview(DockNode node, Window owner, double startX, double startY) {
    super();
    this.node = node;
    this.owner = owner;
    buildUI(startX, startY);
  }

  private void buildUI(double startX, double startY) {
    initOwner(owner);
    setX(startX - FLOATING_NODE_DROPSHADOW_RADIUS);
    setY(startY - FLOATING_NODE_DROPSHADOW_RADIUS);
    createContainerPanel();
    initStyle(StageStyle.TRANSPARENT);
    Scene scene = new Scene(transparentRootPanel,
        node.getWidth() + FLOATING_NODE_DROPSHADOW_RADIUS * 2,
        node.getHeight() + FLOATING_NODE_DROPSHADOW_RADIUS * 2,
        Color.TRANSPARENT);
    setOnShown(e -> {
      setWidth(
          getWidth() + stackPanelContainer.getPadding().getLeft() + stackPanelContainer.getPadding()
              .getRight());
      setHeight(
          getHeight() + stackPanelContainer.getPadding().getTop() + stackPanelContainer.getPadding()
              .getBottom());
    });
    setScene(scene);
  }

  private void createContainerPanel() {
    WritableImage ghostImage = node.snapshot(null, null);
    ImageView imageView = new ImageView(ghostImage);
    stackPanelContainer = new StackPane(imageView);
    transparentRootPanel = new StackPane(stackPanelContainer);
    transparentRootPanel.setPadding(new Insets(FLOATING_NODE_DROPSHADOW_RADIUS));
    transparentRootPanel.setStyle("-fx-background-color:rgba(0,0,0,0);");
    stackPanelContainer.getStyleClass().add("docknode-floating-stack-container-panel");
    stackPanelContainer.setEffect(
        new DropShadow(BlurType.GAUSSIAN, new Color(0, 0, 0, 0.6), FLOATING_NODE_DROPSHADOW_RADIUS,
            0.2, 0, 0));
    stackPanelContainer.relocate(FLOATING_NODE_DROPSHADOW_RADIUS, FLOATING_NODE_DROPSHADOW_RADIUS);
  }

  public void move(double x, double y) {
    setX(x);
    setY(y);
  }

  public void closeStage() {
    transparentRootPanel.getChildren().removeAll();
    setScene(null);
    hide();
  }
}
