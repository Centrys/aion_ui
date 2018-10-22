package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.TokenDetails;
import org.aion.wallet.events.AccountEvent;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;
import java.util.ResourceBundle;

public class TokenBalanceController implements Initializable {
    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();
    private AccountDTO account;

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
    @FXML
    private TextField customTokenSymbol;
    @FXML
    private TextField customTokenDecimals;
    @FXML
    private ScrollPane tokenBalancesScrollPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerEventBusConsumer();
        displayListOfBalances();
    }

    @Subscribe
    private void handleAccountChanged(final AccountEvent event) {
        if (EnumSet.of(AccountEvent.Type.CHANGED, AccountEvent.Type.ADDED).contains(event.getType())) {
            this.account = event.getPayload();
        } else if (AccountEvent.Type.LOCKED.equals(event.getType())) {
            if (event.getPayload().equals(account)) {
                account = null;
            }
        }
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(AccountEvent.ID).register(this);
    }

    public void open(MouseEvent mouseEvent) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        Pane tokenBalanceDialog;
        try {
            tokenBalanceDialog = FXMLLoader.load(getClass().getResource("TokenBalance.fxml"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }

        Node eventSource = (Node) mouseEvent.getSource();
        final double windowX = eventSource.getScene().getWindow().getX();
        final double windowY = eventSource.getScene().getWindow().getY();
        popup.setX(windowX + eventSource.getScene().getWidth() / 1.07 - tokenBalanceDialog.getPrefWidth() / 1.07);
        popup.setY(windowY + eventSource.getScene().getHeight() / 4.75 - tokenBalanceDialog.getPrefHeight() / 4.75);
        popup.getContent().addAll(tokenBalanceDialog);
        popup.show(eventSource.getScene().getWindow());
    }

    public void close(InputEvent eventSource) {
        ((Node) eventSource.getSource()).getScene().getWindow().hide();
    }


    private void displayListOfBalances() {
        List<TokenDetails> accountTokenDetails = blockchainConnector.getAccountTokenDetails(blockchainConnector.getAccountManager().getAccounts().stream().filter(p -> p.isActive()).findFirst().get().getPublicAddress());
        if(accountTokenDetails.size() > 0) {
            for (TokenDetails tokenDetails : accountTokenDetails) {
                HBox row = new HBox();
                row.setSpacing(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPrefWidth(290);
                row.getStyleClass().add("transaction-row");

                Label tokenSymbol = new Label(tokenDetails.getSymbol());
                tokenSymbol.setPrefWidth(70);
                tokenSymbol.getStyleClass().add("transaction-row-text");
                row.getChildren().add(tokenSymbol);

                Label tokenBalance = new Label("1000");
                tokenBalance.getStyleClass().add("transaction-row-text");
                row.getChildren().add(tokenBalance);

                tokenBalances.getChildren().add(row);
            }
        }
        else {
            tokenBalancesScrollPane.setVisible(false);
        }
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

        customTokenContractAddress.setText("");
        customTokenSymbol.setText("");
        customTokenDecimals.setText("");
    }

    public void saveCustomToken(MouseEvent mouseEvent) {
        TokenDetails newToken = new TokenDetails(customTokenContractAddress.getText(), customTokenSymbol.getText(), Integer.valueOf(customTokenDecimals.getText()));
        blockchainConnector.saveToken(newToken);
        blockchainConnector.addAccountToken(blockchainConnector.getAccountManager().getAccounts().stream().filter(p -> p.isActive()).findFirst().get().getPublicAddress(), newToken.getSymbol());

        tokenBalancePane.setPrefHeight(300);
        customTokenForm.setVisible(false);
        customTokenLink.setVisible(true);

        customTokenContractAddress.setText("");
        customTokenSymbol.setText("");
        customTokenDecimals.setText("");

        reloadTokenList();
    }

    private void reloadTokenList() {
        tokenBalances.getChildren().clear();
        displayListOfBalances();
    }
}
