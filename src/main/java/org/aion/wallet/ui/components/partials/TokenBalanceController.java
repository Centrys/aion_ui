package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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

    public void closeTokenBalance(MouseEvent mouseEvent) {
        tokenBalancePane.setVisible(false);
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
        row.setAlignment(Pos.CENTER);
        row.getStyleClass().add("transaction-row");

        Label tokenSymbol = new Label("AION");
        tokenSymbol.setPrefWidth(70);
        tokenSymbol.getStyleClass().add("transaction-row-text");
        row.getChildren().add(tokenSymbol);

        Label tokenBalance = new Label("1000");
        //tokenBalance.setPrefWidth(200);
        tokenBalance.getStyleClass().add("transaction-row-text");
        row.getChildren().add(tokenBalance);

        tokenBalances.getChildren().add(row);
    }
}
