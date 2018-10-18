package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class TokenBalanceController implements Initializable {

    @FXML
    private AnchorPane tokenBalancePane;
    @FXML
    private VBox tokenBalances;
    @FXML
    private VBox customTokenForm;
    @FXML
    private Label customTokenLink;
    @FXML
    private TextField customTokenContractAddress;

    public void closeTokenBalance(MouseEvent mouseEvent) {
        tokenBalancePane.setVisible(false);
        tokenBalancePane.setPrefHeight(250);
        customTokenForm.setVisible(false);
        customTokenLink.setVisible(true);

        Label source = (Label) mouseEvent.getSource();
        HBox h = (HBox) source.getParent();
        VBox v = (VBox) h.getParent();
        AnchorPane a = (AnchorPane) v.getParent();
        AnchorPane aMare = (AnchorPane) a.getParent();
        aMare.getChildren().get(4).setVisible(true);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        displayListOfBalances();
    }

    private void displayListOfBalances() {
        HBox row = new HBox();
        row.setSpacing(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefWidth(290);
        row.getStyleClass().add("transaction-row");

        Label tokenSymbol = new Label("AION");
        tokenSymbol.setPrefWidth(70);
        tokenSymbol.getStyleClass().add("transaction-row-text");
        row.getChildren().add(tokenSymbol);

        Label tokenBalance = new Label("1000");
        tokenBalance.getStyleClass().add("transaction-row-text");
        row.getChildren().add(tokenBalance);

        HBox row1 = new HBox();
        row1.setSpacing(10);
        row1.setAlignment(Pos.CENTER_LEFT);
        row1.setPrefWidth(290);
        row1.getStyleClass().add("transaction-row");

        Label tokenSymbol1 = new Label("TOKEN");
        tokenSymbol1.setPrefWidth(70);
        tokenSymbol1.getStyleClass().add("transaction-row-text");
        row1.getChildren().add(tokenSymbol1);

        Label tokenBalance1 = new Label("5");
        tokenBalance1.getStyleClass().add("transaction-row-text");
        row1.getChildren().add(tokenBalance1);

        tokenBalances.getChildren().add(row);
        tokenBalances.getChildren().add(row1);
    }

    public void addCustomToken(MouseEvent mouseEvent) {
        tokenBalancePane.setPrefHeight(400);
        customTokenForm.setVisible(true);
        customTokenLink.setVisible(false);
    }

    public void cancelCustomToken(MouseEvent mouseEvent) {
        customTokenForm.setVisible(false);
        customTokenLink.setVisible(true);
        tokenBalancePane.setPrefHeight(300);
    }
}
