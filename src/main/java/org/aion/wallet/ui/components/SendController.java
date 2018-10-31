package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.aion.api.impl.internal.Message;
import org.aion.api.log.LogEnum;
import org.aion.base.util.TypeConverter;
import org.aion.mcf.vm.Constants;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendTransactionDTO;
import org.aion.wallet.connector.dto.TransactionResponseDTO;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.TokenDetails;
import org.aion.wallet.events.*;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.ui.components.partials.TransactionResubmissionDialog;
import org.aion.wallet.util.*;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SendController extends AbstractController {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String PENDING_MESSAGE = "Sending transaction...";

    private static final String SUCCESS_MESSAGE = "Transaction finished";

    private static final Tooltip TO_ADDRESS_TOOLTIP = new Tooltip("This is the address that will receive the funds that you are sending");

    private static final Tooltip NRG_LIMIT_TOOLTIP = new Tooltip("Energy limit represents the amount of energy applied to this transaction");

    private static final Tooltip NRG_PRICE_TOOLTIP = new Tooltip("Energy price expressed in nAmp");

    private static final Tooltip AMOUNT_TOOLTIP = new Tooltip("This is the amount that the address specified above will receive");

    private static final String SEPARATOR = "-----";

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    private final TransactionResubmissionDialog transactionResubmissionDialog = new TransactionResubmissionDialog();

    @FXML
    private TextField toInput;
    @FXML
    private TextField nrgInput;
    @FXML
    private TextField nrgPriceInput;
    @FXML
    private TextField valueInput;
    @FXML
    private Label txStatusLabel;
    @FXML
    private TextArea accountAddress;
    @FXML
    private TextField accountBalance;
    @FXML
    private Button sendButton;
    @FXML
    private Label timedOutTransactionsLabel;
    @FXML
    private ComboBox<String> currencySelect;

    private AccountDTO account;
    private boolean connected;
    private SendTransactionDTO transactionToResubmit;

    @Override
    protected void registerEventBusConsumer() {
        super.registerEventBusConsumer();
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
        EventBusFactory.getBus(AccountEvent.ID).register(this);
        EventBusFactory.getBus(TransactionEvent.ID).register(this);
        EventBusFactory.getBus(UiMessageEvent.ID).register(this);
    }

    @Override
    protected void internalInit(final URL location, final ResourceBundle resources) {
        TO_ADDRESS_TOOLTIP.setPrefWidth(200);
        TO_ADDRESS_TOOLTIP.setWrapText(true);
        NRG_LIMIT_TOOLTIP.setPrefWidth(200);
        NRG_LIMIT_TOOLTIP.setWrapText(true);
        NRG_PRICE_TOOLTIP.setPrefWidth(200);
        NRG_PRICE_TOOLTIP.setWrapText(true);
        AMOUNT_TOOLTIP.setPrefWidth(200);
        AMOUNT_TOOLTIP.setWrapText(true);

        toInput.setTooltip(TO_ADDRESS_TOOLTIP);
        nrgInput.setTooltip(NRG_LIMIT_TOOLTIP);
        nrgPriceInput.setTooltip(NRG_PRICE_TOOLTIP);
        valueInput.setTooltip(AMOUNT_TOOLTIP);
        setDefaults();

        toInput.textProperty().addListener(event -> transactionToResubmit = null);

        nrgInput.textProperty().addListener(event -> transactionToResubmit = null);

        nrgPriceInput.textProperty().addListener(event -> transactionToResubmit = null);

        valueInput.textProperty().addListener(event -> transactionToResubmit = null);
    }

    @Override
    protected void refreshView(final RefreshEvent event) {
        switch (event.getType()) {
            case CONNECTED:
                connected = true;
                if (account != null) {
                    sendButton.setDisable(false);
                }
                break;
            case DISCONNECTED:
                connected = false;
                sendButton.setDisable(true);
                break;
            case TRANSACTION_FINISHED:
                setDefaults();
                refreshAccountBalance();
                break;
            default:
        }
        setTimedOutTransactionsLabelText();
    }

    @FXML
    private void addressPressed(final KeyEvent keyEvent) {
        if (account != null) {
            switch (keyEvent.getCode()) {
                case TAB:
                    try {
                        checkAddress();
                        displayStatus("", false);
                    } catch (ValidationException e) {
                        displayStatus(e.getMessage(), true);
                    }
                    break;
            }
        }
    }

    @FXML
    private void nrgPressed(final KeyEvent keyEvent) {
        if (account != null) {
            switch (keyEvent.getCode()) {
                case TAB:
                    try {
                        getNrg();
                        displayStatus("", false);
                    } catch (ValidationException e) {
                        displayStatus(e.getMessage(), true);
                    }
                    break;
            }
        }
    }

    @FXML
    private void nrgPricePressed(final KeyEvent keyEvent) {
        if (account != null) {
            switch (keyEvent.getCode()) {
                case TAB:
                    try {
                        getNrgPrice();
                        displayStatus("", false);
                    } catch (ValidationException e) {
                        displayStatus(e.getMessage(), true);
                    }
                    break;
            }
        }
    }

    @FXML
    private void valuePressed(final KeyEvent keyEvent) {
        if (account != null) {
            switch (keyEvent.getCode()) {
                case ENTER:
                    sendAion();
                    break;
                case TAB:
                    try {
                        getValue();
                        displayStatus("", false);
                    } catch (ValidationException e) {
                        displayStatus(e.getMessage(), true);
                    }
                    break;
            }
        }
    }

    @FXML
    private void sendPressed(final KeyEvent keyEvent) {
        if (account != null) {
            switch (keyEvent.getCode()) {
                case ENTER:
                    sendAion();
                    break;
            }
        }
    }

    public void sendAion() {
        if (account == null) {
            return;
        }
        final SendTransactionDTO dto;
        try {
            if (transactionToResubmit != null) {
                dto = transactionToResubmit;
            } else {
                dto = mapFormData();
            }
        } catch (ValidationException e) {
            log.error(e.getMessage(), e);
            displayStatus(e.getMessage(), true);
            return;
        }
        displayStatus(PENDING_MESSAGE, false);

        final Task<TransactionResponseDTO> sendTransactionTask = getApiTask(this::sendTransaction, dto);

        runApiTask(
                sendTransactionTask,
                evt -> handleTransactionFinished(sendTransactionTask.getValue()),
                getErrorEvent(t -> Optional.ofNullable(t.getCause()).ifPresent(cause -> displayStatus(cause.getMessage(), true)), sendTransactionTask),
                getEmptyEvent()
        );
    }

    public void onTimedOutTransactionsClick(final MouseEvent mouseEvent) {
        transactionResubmissionDialog.open(mouseEvent);
    }

    private void handleTransactionFinished(final TransactionResponseDTO response) {
        setTimedOutTransactionsLabelText();
        final String error = response.getError();
        if (error != null) {
            final String failReason;
            final int responseStatus = response.getStatus();
            if (Message.Retcode.r_tx_Dropped_VALUE == responseStatus) {
                failReason = String.format("dropped: %s", error);
            } else {
                failReason = "timeout";
            }
            final String errorMessage = "Transaction " + failReason;
            ConsoleManager.addLog(errorMessage, ConsoleManager.LogType.TRANSACTION, ConsoleManager.LogLevel.WARNING);
            SendController.log.error("{}: {}", errorMessage, response);
            displayStatus(errorMessage, false);
        } else {
            log.info("{}: {}", SUCCESS_MESSAGE, response);
            ConsoleManager.addLog("Transaction sent", ConsoleManager.LogType.TRANSACTION, ConsoleManager.LogLevel.WARNING);
            displayStatus(SUCCESS_MESSAGE, false);
            EventPublisher.fireTransactionFinished();
        }
    }

    private void displayStatus(final String message, final boolean isError) {
        final ConsoleManager.LogLevel logLevel;
        if (isError) {
            txStatusLabel.getStyleClass().add(ERROR_STYLE);
            logLevel = ConsoleManager.LogLevel.ERROR;

        } else {
            txStatusLabel.getStyleClass().removeAll(ERROR_STYLE);
            logLevel = ConsoleManager.LogLevel.INFO;
        }
        txStatusLabel.setText(message);
        ConsoleManager.addLog(message, ConsoleManager.LogType.TRANSACTION, logLevel);
    }

    private TransactionResponseDTO sendTransaction(final SendTransactionDTO sendTransactionDTO) {
        try {
            return blockchainConnector.sendTransaction(sendTransactionDTO);
        } catch (ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setDefaults() {
        nrgInput.setText(String.valueOf(Constants.NRG_TRANSACTION));
        nrgPriceInput.setText(AionConstants.DEFAULT_NRG_PRICE.toString());

        toInput.setText("");
        valueInput.setText("");

        setTimedOutTransactionsLabelText();
    }

    private void setTimedOutTransactionsLabelText() {
        if (account != null) {
            final List<SendTransactionDTO> timedOutTransactions = blockchainConnector.getAccountManager().getTimedOutTransactions(account.getPublicAddress());
            if (!timedOutTransactions.isEmpty()) {
                timedOutTransactionsLabel.setVisible(true);
                timedOutTransactionsLabel.getStyleClass().add("warning-link-style");
                timedOutTransactionsLabel.setText("You have transactions that require your attention!");
            }
        } else {
            timedOutTransactionsLabel.setVisible(false);
        }
    }

    @Subscribe
    private void handleAccountEvent(final AccountEvent event) {
        final AccountDTO account = event.getPayload();
        if (AccountEvent.Type.CHANGED.equals(event.getType())) {
            connected = blockchainConnector.isConnected();
            if (account.isActive()) {
                this.account = account;
                sendButton.setDisable(!connected);
                accountAddress.setText(this.account.getPublicAddress());
                accountBalance.setVisible(true);
                setAccountBalanceText();
            }
            currencySelect.setDisable(false);
            currencySelect.setItems(getCurrencySymbols(account));
            currencySelect.getSelectionModel().select(0);
        } else if (AccountEvent.Type.LOCKED.equals(event.getType())) {
            if (account.equals(this.account)) {
                this.account = null;
                sendButton.setDisable(true);
                accountAddress.setText("");
                accountBalance.setVisible(false);
                setDefaults();
                txStatusLabel.setText("");
            }
            currencySelect.setDisable(true);
        }
    }

    private ObservableList<String> getCurrencySymbols(AccountDTO account) {
        ObservableList<String> result = FXCollections.observableArrayList();

        List<String> tokenSymbols = getTokens(account);
        result.add(account.getCurrency());
        result.add(SEPARATOR);
        result.addAll(tokenSymbols);
        return result;
    }

    @Subscribe
    private void handleHeaderPaneButtonEvent(final HeaderPaneButtonEvent event) {
        if (event.getType().equals(HeaderPaneButtonEvent.Type.SEND)) {
            refreshAccountBalance();
        }
    }

    @Subscribe
    private void handleTransactionResubmitEvent(final TransactionEvent event) {
        SendTransactionDTO sendTransaction = event.getTransaction();
        sendTransaction.setNrgPrice(BigInteger.valueOf(sendTransaction.getNrgPrice() * 2));
        toInput.setText(sendTransaction.getTo());
        nrgInput.setText(sendTransaction.getNrg().toString());
        nrgPriceInput.setText(String.valueOf(sendTransaction.getNrgPrice()));
        valueInput.setText(BalanceUtils.formatBalance(sendTransaction.getValue()));
        txStatusLabel.setText("");
        timedOutTransactionsLabel.setVisible(false);
        transactionToResubmit = sendTransaction;
    }

    @Subscribe
    private void handleTokenAddedEvent(final UiMessageEvent event) {
        if (UiMessageEvent.Type.TOKEN_ADDED.equals(event.getType())) {
            currencySelect.getItems().clear();
            currencySelect.setItems(getCurrencySymbols(account));
            currencySelect.getSelectionModel().select(0);
        }
    }

    private List<String> getTokens(final AccountDTO account) {
        return blockchainConnector.getAccountTokenDetails(account.getPublicAddress()).stream().map(TokenDetails::getSymbol).collect(Collectors.toList());
    }

    private void setAccountBalanceText() {
        accountBalance.setText(account.getFormattedBalance() + " " + AionConstants.CCY);
        UIUtils.setWidth(accountBalance);
    }

    private void refreshAccountBalance() {
        if (account == null) {
            return;
        }
        final Task<BigInteger> getBalanceTask = getApiTask(blockchainConnector::getBalance, account.getPublicAddress());
        runApiTask(
                getBalanceTask,
                evt -> {
                    account.setBalance(getBalanceTask.getValue());
                    setAccountBalanceText();
                },
                getErrorEvent(t -> {}, getBalanceTask),
                getEmptyEvent()
        );
    }

    private SendTransactionDTO mapFormData() throws ValidationException {

        checkAddress();

        final long nrg = getNrg();

        final BigInteger nrgPrice = getNrgPrice();

        final BigInteger value = getValue();

        final Optional<TokenDetails> tokenDetailsOptional = getTokenDetailsOptional();

        return tokenDetailsOptional.map(
                tokenDetails -> {
                    final String tokenAddress = tokenDetails.getContractAddress();
                    final byte[] sendData = blockchainConnector.getTokenSendData(tokenAddress, account.getPublicAddress(), toInput.getText(), value);
                    final long estimatedNrg = getEstimatedNrg(sendData);
                    return new SendTransactionDTO(account, tokenAddress, estimatedNrg, nrgPrice, BigInteger.ZERO, sendData);
                })
                .orElseGet(() -> new SendTransactionDTO(account, toInput.getText(), nrg, nrgPrice, value));
    }

    private long getEstimatedNrg(final byte[] sendData) {
        final long zeroes = zeroBytesInData(sendData);
        final long nonZeroes = nonZeroBytesInData(sendData);
        final long energy = Constants.NRG_TRANSACTION + zeroes * Constants.NRG_TX_DATA_ZERO + nonZeroes * Constants.NRG_TX_DATA_NONZERO;
        return (Math.floorDiv(energy, 1000) + 1) * 1000;
    }

    private Optional<TokenDetails> getTokenDetailsOptional() throws ValidationException {
        final String tokenSymbol = currencySelect.getSelectionModel().getSelectedItem();
        if (!tokenSymbol.equals(account.getCurrency())) {
            final List<TokenDetails> tokenDetails = blockchainConnector.getAccountTokenDetails(account.getPublicAddress());
            final Optional<TokenDetails> matchingTokenOptional = tokenDetails.stream()
                    .filter(t -> tokenSymbol.equals(t.getSymbol()))
                    .findFirst();
            if (!matchingTokenOptional.isPresent()) {
                throw new ValidationException("The selected currency is not valid!");
            } else {
                if (getTokenBalance(matchingTokenOptional.get()).compareTo(getValue()) < 0) {
                    throw new ValidationException("There are not enough " + tokenSymbol + " tokens");
                } else {
                    return matchingTokenOptional;
                }
            }
        } else {
            return Optional.empty();
        }
    }

    private BigInteger getTokenBalance(TokenDetails tokenAddress) throws ValidationException {
        return blockchainConnector.getTokenBalance(tokenAddress.getContractAddress(), account.getPublicAddress());
    }

    private void checkAddress() throws ValidationException {
        if (!AddressUtils.isValid(toInput.getText())) {
            throw new ValidationException("Address is not a valid AION address!");
        }
    }

    private long getNrg() throws ValidationException {
        final long nrg;
        try {
            nrg = TypeConverter.StringNumberAsBigInt(nrgInput.getText()).longValue();
            if (nrg <= 0) {
                throw new ValidationException("Nrg must be greater than 0!");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Nrg must be a valid number!");
        }
        return nrg;
    }

    private BigInteger getNrgPrice() throws ValidationException {
        final BigInteger nrgPrice;
        try {
            nrgPrice = TypeConverter.StringNumberAsBigInt(nrgPriceInput.getText());
            if (nrgPrice.compareTo(AionConstants.DEFAULT_NRG_PRICE) < 0) {
                throw new ValidationException(String.format("Nrg price must be greater than %s!", AionConstants.DEFAULT_NRG_PRICE));
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Nrg price must be a valid number!");
        }
        return nrgPrice;
    }

    private BigInteger getValue() throws ValidationException {
        final BigInteger value;
        try {
            value = BalanceUtils.extractBalance(valueInput.getText());
            if (value.compareTo(BigInteger.ZERO) <= 0) {
                throw new ValidationException("Amount must be greater than 0");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Amount must be a number");
        }
        return value;
    }

    public void showInfoTooltip(final MouseEvent mouseEvent) {
        switch (((Node) mouseEvent.getSource()).getId()) {
            case "toAddressInfoPane":
                TO_ADDRESS_TOOLTIP.show((Node) mouseEvent.getSource(), ((Node) mouseEvent.getSource()).getScene().getWindow().getX() + 600, ((Node) mouseEvent.getSource()).getScene().getWindow().getY() + 220);
                break;
            case "energyInfoPane":
                NRG_LIMIT_TOOLTIP.show((Node) mouseEvent.getSource(), ((Node) mouseEvent.getSource()).getScene().getWindow().getX() + 600, ((Node) mouseEvent.getSource()).getScene().getWindow().getY() + 265);
                break;
            case "energyPriceInfoPane":
                NRG_PRICE_TOOLTIP.show((Node) mouseEvent.getSource(), ((Node) mouseEvent.getSource()).getScene().getWindow().getX() + 600, ((Node) mouseEvent.getSource()).getScene().getWindow().getY() + 315);
                break;
            case "amountInfoPane":
                AMOUNT_TOOLTIP.show((Node) mouseEvent.getSource(), ((Node) mouseEvent.getSource()).getScene().getWindow().getX() + 600, ((Node) mouseEvent.getSource()).getScene().getWindow().getY() + 330);
                break;
        }
    }

    public void hideInfoTooltip(final MouseEvent mouseEvent) {
        switch (((Node) mouseEvent.getSource()).getId()) {
            case "toAddressInfoPane":
                TO_ADDRESS_TOOLTIP.hide();
                break;
            case "energyInfoPane":
                NRG_LIMIT_TOOLTIP.hide();
                break;
            case "energyPriceInfoPane":
                NRG_PRICE_TOOLTIP.hide();
                break;
            case "amountInfoPane":
                AMOUNT_TOOLTIP.hide();
                break;
        }
    }

    private long nonZeroBytesInData(final byte[] data) {
        int total = (data == null) ? 0 : data.length;

        return total - zeroBytesInData(data);
    }

    private long zeroBytesInData(final byte[] data) {
        if (data == null) {
            return 0;
        }

        int zeroes = 0;
        for (byte b : data) {
            zeroes += (b == 0) ? 1 : 0;
        }
        return zeroes;
    }

    @FXML
    private void coinChanged(final ActionEvent actionEvent) {
        final String selectedItem = currencySelect.getSelectionModel().getSelectedItem();
        if (SEPARATOR.equals(selectedItem)) {
            nrgInput.setText("");
        } else if (getTokens(account).contains(selectedItem)) {
            nrgInput.setText(String.valueOf(AionConstants.DEFAULT_TOKEN_NRG));
        } else {
            nrgInput.setText(String.valueOf(AionConstants.DEFAULT_NRG));
        }
    }
}
