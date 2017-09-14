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
package com.anchorage.docks.node.ui;

import java.util.Objects;

import com.anchorage.docks.containers.NodeDraggingPreview;
import com.anchorage.docks.node.DockNode;

import javafx.beans.property.StringProperty;
import javafx.geometry.Point2D;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * @author Alessio
 */
public final class DockUIPanel extends Pane {

  public static final double BAR_HEIGHT = 25;

  private Node nodeContent;
  private Label titleLabel;
  private Pane barPanel;
  private StackPane contentPanel;
  private DockCommandsBox commandsBox;
  private DockNode node;
  private Point2D deltaDragging;
  private boolean substationType;
  private Node iconView;
  private NodeDraggingPreview nodePreview;

  
  public DockUIPanel(String title, Node _nodeContent, boolean _substationType, Image icon) {
      this(title, _nodeContent, _substationType, new ImageView(icon));
  }
  
  public DockUIPanel(String title, Node _nodeContent, boolean _substationType, Node icon) {
      if (icon instanceof ImageView) {
          alignImageView((ImageView) icon);
      }

      getStylesheets().add("AnchorFX.css");
      substationType = _substationType;
      Objects.requireNonNull(_nodeContent);
      Objects.requireNonNull(title);
      nodeContent = _nodeContent;
      buildNode(title, icon);
      //    installDragEventManager(barPanel);
  }

  public Node getNodeForDraggingManagement() {
    return barPanel;
  }

  public void setIcon(Image icon) {
      ImageView imgView = new ImageView(icon);
      alignImageView(imgView);
      setIcon(imgView);
  }
  
  public void setIcon(Node icon) {
      Objects.requireNonNull(icon);
      if (barPanel.getChildren().contains(iconView)) {
          barPanel.getChildren().remove(iconView);
      }
      if (icon instanceof ImageView) {
          alignImageView((ImageView) icon);
      }
      iconView = icon;
      iconView.relocate(5, (BAR_HEIGHT - iconView.getLayoutBounds().getHeight()) / 2 + 1);
      barPanel.getChildren().add(0, iconView);
    }
  
  private static void alignImageView(final ImageView imgView) {
      imgView.setFitWidth(15);
      imgView.setFitHeight(15);
      imgView.setPreserveRatio(false);
      imgView.setSmooth(true);
  }

  private void makeCommands() {
    commandsBox = new DockCommandsBox(node);
    barPanel.getChildren().add(commandsBox);
    commandsBox.layoutXProperty()
        .bind(barPanel.prefWidthProperty().subtract(commandsBox.getChildren().size() * 30 + 10));
    commandsBox.setLayoutY(0);
    titleLabel.prefWidthProperty().bind(commandsBox.layoutXProperty().subtract(10));
  }

  public void setDockNode(DockNode node) {
    this.node = node;
    makeCommands();
  }

  public StringProperty titleProperty() {
    return titleLabel.textProperty();
  }

//  public void installDragEventManager(Node n) {
//    n.setOnMouseClicked(event -> {
//      if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
//        node.maximizeOrRestore();
//      }
//    });
//    n.setOnMouseDragged(event -> {
//      if (event.getButton() == MouseButton.PRIMARY) {
//        manageDragEvent(event);
//      }
//    });
//    n.setOnMouseReleased(event -> {
//      if (event.getButton() == MouseButton.PRIMARY) {
//        manageReleaseEvent();
//      }
//    });
//  }

//  private void manageDragEvent(MouseEvent event) {
//    if (node.maximizingProperty().get()) {
//      return;
//    }
//    if (!node.draggingProperty().get()) {
//
//      Bounds bounds = node.localToScreen(barPanel.getBoundsInLocal());
//      deltaDragging = new Point2D(event.getScreenX() - bounds.getMinX(),
//          event.getScreenY() - bounds.getMinY());
//      showDraggedNodePreview(event.getScreenX() - deltaDragging.getX(),
//          event.getScreenY() - deltaDragging.getY());
//      if (!node.maximizingProperty().get()) {
//        AnchorageSystem.prepareDraggingZoneFor(node.stationProperty().get(), node);
//      }
//    } else {
//      if (node.draggingProperty().get()) {
////          node.moveFloatable(event.getScreenX() - deltaDragging.getX(),
////              event.getScreenY() - deltaDragging.getY());
//        nodePreview.setX(event.getScreenX() - deltaDragging.getX());
//        nodePreview.setY(event.getScreenY() - deltaDragging.getY());
//        node.stationProperty().get().searchTargetNode(event.getScreenX(), event.getScreenY());
//        AnchorageSystem.searchTargetNode(event.getScreenX(), event.getScreenY());
//
//      }
//    }
//  }

//  private void showDraggedNodePreview(double x, double y) {
//    node.enableDragging();
//    if (nodePreview != null) {
//      nodePreview.closeStage();
//    }
//    nodePreview = new NodeDraggingPreview(node, node.stationProperty().get().getScene().getWindow(),
//        x, y);
//    nodePreview.show();
//  }

//  private void manageReleaseEvent() {
//    if (node.draggingProperty().get() && !node.maximizingProperty().get()) {
//      AnchorageSystem.finalizeDragging();
//      nodePreview.closeStage();
//      nodePreview = null;
//    }
//  }

  private void buildNode(String title, Node icon) {
    Objects.requireNonNull(icon);
    Objects.requireNonNull(title);
    barPanel = new Pane();
    String titleBarStyle = (!substationType) ? "docknode-title-bar" : "substation-title-bar";
    barPanel.getStyleClass().add(titleBarStyle);
    barPanel.setPrefHeight(BAR_HEIGHT);
    barPanel.relocate(0, 0);
    barPanel.prefWidthProperty().bind(widthProperty());
    titleLabel = new Label(title);
    String titleTextStyle = (!substationType) ? "docknode-title-text" : "substation-title-text";
    iconView = icon; 
    iconView.relocate(5, (BAR_HEIGHT - iconView.getLayoutBounds().getHeight()) / 2 + 1);
    titleLabel.getStyleClass().add(titleTextStyle);
    barPanel.getChildren().addAll(iconView, titleLabel);
    titleLabel.relocate(25, 5);
    contentPanel = new StackPane();
    contentPanel.getStyleClass().add("docknode-content-panel");
    contentPanel.relocate(0, BAR_HEIGHT);
    contentPanel.prefWidthProperty().bind(widthProperty());
    contentPanel.prefHeightProperty().bind(heightProperty().subtract(BAR_HEIGHT));
    contentPanel.getChildren().add(nodeContent);
    contentPanel.setCache(true);
    contentPanel.setCacheHint(CacheHint.SPEED);
    if (nodeContent instanceof Pane) {
      Pane nodeContentPane = (Pane) nodeContent;
      nodeContentPane.setMinHeight(USE_COMPUTED_SIZE);
      nodeContentPane.setMinWidth(USE_COMPUTED_SIZE);
      nodeContentPane.setMaxWidth(USE_COMPUTED_SIZE);
      nodeContentPane.setMaxHeight(USE_COMPUTED_SIZE);
    }
    getChildren().addAll(barPanel, contentPanel);
  }

  public StackPane getContentContainer() {
    return contentPanel;
  }

  /**
   * Get the value of nodeContent
   *
   * @return the value of nodeContent
   */
  public Node getNodeContent() {
    return nodeContent;
  }

  public boolean isMenuButtonEnable() {
    return commandsBox.isMenuButtonEnable();
  }

}
