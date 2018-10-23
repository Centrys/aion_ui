package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.fxml.FXML;
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
import javafx.scene.layout.VBox;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.dto.TokenDetails;
import org.aion.wallet.events.AccountEvent;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.events.UiMessageEvent;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.AddressUtils;
import org.aion.wallet.util.BalanceUtils;
import org.slf4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TokenBalanceController implements Initializable {
    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

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
    private Label customTokenValidation;
    @FXML
    private ScrollPane tokenBalancesScrollPane;

    private String accountAddress;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerEventBusConsumer();
    }

    @Subscribe
    private void handleAccountChanged(final AccountEvent event) {
        if (AccountEvent.Type.LOCKED.equals(event.getType())) {
            if (event.getPayload().getPublicAddress().equals(accountAddress)) {
                accountAddress = null;
            }
            tokenBalancePane.setVisible(false);
        }
    }

    @Subscribe
    private void handleOpenRequest(final UiMessageEvent event) {
        if (UiMessageEvent.Type.TOKEN_BALANCES_SHOW.equals(event.getType())) {
            accountAddress = event.getMessage();
            backgroundExecutor.submit(() -> Platform.runLater(this::displayListOfBalances));
        }
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(AccountEvent.ID).register(this);
        EventBusFactory.getBus(UiMessageEvent.ID).register(this);
    }

    public void close(InputEvent eventSource) {
        ((Node) eventSource.getSource()).getScene().getWindow().hide();
    }


    private void displayListOfBalances() {
        List<TokenDetails> accountTokenDetails = blockchainConnector.getAccountTokenDetails(accountAddress);
        if (accountTokenDetails.size() > 0) {
            tokenBalancesScrollPane.setVisible(true);
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

                String balance = null;
                try {
                    balance = BalanceUtils.formatBalanceWithNumberOfDecimals(
                            blockchainConnector.getTokenBalance(accountAddress, tokenDetails), 6
                    );
                } catch (ValidationException e) {
                    log.error(e.getMessage());
                }
                Label tokenBalance = new Label(balance);
                tokenBalance.getStyleClass().add("transaction-row-text");
                row.getChildren().add(tokenBalance);

                tokenBalances.getChildren().add(row);
            }
        } else {
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

        customTokenValidation.setVisible(false);
        customTokenValidation.setText("");
    }

    public void saveCustomToken(MouseEvent mouseEvent) {
        customTokenValidation.setVisible(false);
        customTokenValidation.setText("");

        try {
            checkTokenContractAddress();
            checkTokenSymbol();
            checkTokenDecimals();
        }
        catch (ValidationException exception) {
            customTokenValidation.setVisible(true);
            customTokenValidation.setText(exception.getMessage());
            return;
        }

        TokenDetails newToken = new TokenDetails(
                customTokenContractAddress.getText(),
                customTokenSymbol.getText(),
                Integer.parseInt(customTokenDecimals.getText())
        );
        blockchainConnector.saveToken(newToken);
        blockchainConnector.addAccountToken(accountAddress, newToken.getSymbol());

        tokenBalancePane.setPrefHeight(300);
        customTokenForm.setVisible(false);
        customTokenLink.setVisible(true);

        customTokenContractAddress.setText("");
        customTokenSymbol.setText("");
        customTokenDecimals.setText("");

        reloadTokenList();

        EventPublisher.fireTokenAdded(mouseEvent);
    }

    private void checkTokenContractAddress() throws ValidationException{
        //TODO check that the contract address is valid
        if(!AddressUtils.isValid(customTokenContractAddress.getText())) {
            throw new ValidationException("The contract address is not valid!");
        }
    }

    private void checkTokenSymbol() throws ValidationException{
        //TODO check that the symbol is the actual symbol for this contract address
        if(customTokenSymbol.getText() == null || customTokenSymbol.getText().isEmpty()) {
            throw new ValidationException("The provided token symbol is not valid!");
        }
        ArrayList coinSymbols = new ArrayList<>(Collections.singleton("AION"));
        List<String> tokenSymbols = blockchainConnector.getAccountTokenDetails(accountAddress).stream().map(TokenDetails::getSymbol).collect(Collectors.toList());
        if(coinSymbols.contains(customTokenSymbol.getText()) || tokenSymbols.contains(customTokenSymbol.getText())) {
            throw new ValidationException("Token already exists!");
        }
    }

    private void checkTokenDecimals() throws ValidationException{
        if(customTokenDecimals.getText() == null || customTokenDecimals.getText().isEmpty()) {
            throw new ValidationException("Token decimals has to be a number!");
        }
        else {
            try {
                Double.parseDouble(customTokenDecimals.getText());
            }
            catch (NumberFormatException e) {
                throw new ValidationException("Token decimals has to be a number!");
            }
        }
    }

    private void reloadTokenList() {
        tokenBalances.getChildren().clear();
        displayListOfBalances();
    }
}
