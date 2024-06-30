import Client.ClientController
import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.scene.layout.Priority
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment
import java.lang.Thread.sleep

const val GREEN_STYLE = "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;"
const val RED_STYLE = "-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;"
const val GRAY_STYLE = "-fx-background-color: gray; -fx-text-fill: white; -fx-font-weight: bold;"

class ClientView: Application() {
    private var clientController:ClientController? = null
    private val messageList = ListView<String>()
    private val userList = ListView<String>()
    private val bottomStatus = Label("None")
    private var isLogged = false

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Messenger App"
        showInitialScene(primaryStage)
    }

    private fun showInitialScene(primaryStage: Stage) {
        if (!isLogged) {
            mainMenuScene(primaryStage)
        } else {
            messengerScene(primaryStage)
        }
    }

    private fun mainMenuScene(primaryStage: Stage) {
        val title = Label("Welcome to the App!")
        title.font = Font.font("Arial", FontWeight.BOLD, 24.0)
        title.textAlignment = TextAlignment.CENTER

        val registrationButton = Button("Registration")
        val loginButton = Button("Login")

        registrationButton.style = GRAY_STYLE
        loginButton.style = GRAY_STYLE

        val buttonWidth = 200.0
        val buttonHeight = 40.0

        registrationButton.prefHeight = buttonHeight
        loginButton.prefHeight = buttonHeight
        registrationButton.prefWidth = buttonWidth
        loginButton.prefWidth = buttonWidth

        val userLoginRegistrationButtons = VBox(10.0,title,registrationButton, loginButton)
        userLoginRegistrationButtons.alignment = Pos.CENTER
        userLoginRegistrationButtons.padding = Insets(20.0)

        registrationButton.setOnAction {

            Platform.runLater {
                registrationScene(primaryStage)
            }
        }
        loginButton.setOnAction {

            Platform.runLater {
                loginScene(primaryStage)
            }
        }

        val borderPane = BorderPane()
        borderPane.center = userLoginRegistrationButtons

        val scene = Scene(borderPane, 800.0, 400.0)
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun messengerScene(primaryStage: Stage){
        val thread2 = Thread{ this.runClient() }
        val userNameLabel = Label("Messages")
        clientController!!.connectDb()
        userList.items.addAll(clientController!!.getAllConverClientNames())

        val messageInput = TextField()
        messageInput.promptText = "Write a message..."
        val sendButton = Button("Send")
        sendButton.style = GRAY_STYLE

        val messageInputBox = HBox(messageInput, sendButton)
        messageInputBox.alignment = Pos.CENTER
        messageInputBox.isVisible = false
        HBox.setHgrow(messageInput, Priority.ALWAYS)

        val userNameInput = TextField()
        userNameInput.promptText = "Search user"
        val addUserButton= Button("Add")
        addUserButton.style = GRAY_STYLE
        val deleteUserButton = Button("Delete")
        deleteUserButton.style = RED_STYLE
        val userNameInputBox = HBox(userNameInput, addUserButton,deleteUserButton)
        userNameInputBox.alignment = Pos.CENTER
        HBox.setHgrow(userNameInput, Priority.ALWAYS)

        val logoutButton = Button("Logout")
        logoutButton.style = RED_STYLE
        val activeUserLabel = Label("User name is: ${clientController!!.getName()}")
        activeUserLabel.font = Font.font(18.0)
        val topBox = HBox(5.0,logoutButton, activeUserLabel)

        userList.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            messageInputBox.isVisible = newValue != null
            if (userList.selectionModel.selectedItem!= null) userNameLabel.text=newValue
            this.loadMessagesForUser()
        }

        addUserButton.setOnAction {
            val userName = userNameInput.text.trim()
            if(userName.isNotEmpty() && !userList.items.contains(userName)) {
                userList.items.add(userName)
            }
            userNameInput.clear()
        }

        deleteUserButton.setOnAction {
            val selectedUser = userList.selectionModel.selectedItem
            if (selectedUser != null) {
                clientController!!.deleteAllMessagesWithConvClient(selectedUser)
                messageList.items.clear()
                userNameLabel.text = "Messages"
                userList.items.remove(selectedUser)
            }
        }

        sendButton.setOnAction {
            val selectedUser = userList.selectionModel.selectedItem
            val messageText = messageInput.text
            if (clientController!!.getConnectStatus()) {
                clientController!!.sendMessage(TypeMessage.SEND, messageText, selectedUser)
                loadMessagesForUser()
                messageInput.clear()
            }else{
                Platform.runLater{
                    val alert = Alert(AlertType.WARNING)
                    alert.title = "WARNING!"
                    alert.headerText = "Oops, no connection!"
                    alert.contentText = "Wait for to reconnect to the server."
                    alert.showAndWait()
                }
            }
        }

        logoutButton.setOnAction {
            thread2.interrupt()
            clientController!!.closeDB()
            Platform.runLater{
                userList.items.clear()
                messageList.items.clear()
                mainMenuScene(primaryStage)
            }
        }

        val messagesPane = VBox()
        messagesPane.children.addAll(userNameLabel, messageList, messageInputBox)
        messagesPane.padding = Insets(5.0, 10.0,5.0, 10.0)
        VBox.setVgrow(messageList, Priority.ALWAYS)

        val usersPane = VBox()
        usersPane.children.addAll(userNameInputBox, userList)
        usersPane.padding = Insets(5.0, 10.0,5.0, 10.0)
        VBox.setVgrow(userList, Priority.ALWAYS)

        topBox.padding = Insets(0.0, 10.0, 0.0, 10.0)
        val borderPane = BorderPane()
        borderPane.top = topBox
        borderPane.left = usersPane
        borderPane.center = messagesPane

        bottomStatus.padding = Insets(0.0, 10.0, 5.0, 10.0)
        borderPane.bottom = bottomStatus

        val scene = Scene(borderPane, 800.0, 400.0)
        primaryStage.scene = scene

        primaryStage.show()

        thread2.start()

        primaryStage.setOnCloseRequest {
            thread2.interrupt()
            clientController!!.closeDB()
        }
    }

    private fun createUserForm(
        primaryStage: Stage,
        buttonText: String,
        action: (String, String) -> Unit,
    ): VBox {
        val titleLabel = Label(if (buttonText == "Login!") "Login" else "Register")
        titleLabel.font = Font.font("Arial", FontWeight.BOLD, 24.0)

        val inputUserName = TextField()
        inputUserName.promptText = "Username"
        val inputPassword = PasswordField()
        inputPassword.promptText = "Password"
        val button = Button(buttonText)
        val backButton = Button("Back")

        val width = 200.0
        val height = 40.0

        inputUserName.maxWidth = width
        inputPassword.maxWidth = width
        button.maxWidth = width
        backButton.maxWidth = width

        inputUserName.prefHeight = height
        inputPassword.prefHeight = height
        button.prefHeight = height
        backButton.prefHeight = height

        button.style = GREEN_STYLE
        backButton.style = RED_STYLE

        val userAction = VBox(10.0, titleLabel, inputUserName, inputPassword, button, backButton)
        userAction.alignment = Pos.CENTER

        button.setOnAction {
            val username = inputUserName.text
            val password = inputPassword.text
            Platform.runLater {
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    action(username, password)
                } else {
                    val alert = Alert(AlertType.WARNING)
                    alert.title = "WARNING!"
                    alert.headerText = "Oops!"
                    alert.contentText = "User name field or password is empty!"
                    alert.showAndWait()
                }
            }
        }

        backButton.setOnAction {
            Platform.runLater {
                mainMenuScene(primaryStage)
            }
        }

        return userAction
    }

    private fun loginScene(primaryStage: Stage) {
        val userAction = createUserForm(primaryStage, "Login!") { username, password ->
            val newClientController = ClientController(username, password)
            if (newClientController.loginUser()) {
                clientController = newClientController
                bottomStatus.text = "Connection status: ${if (clientController!!.getConnectStatus()) "Connected" else "Disconnected"}"
                messengerScene(primaryStage)
            } else {
                val alert = Alert(AlertType.ERROR)
                alert.title = "ERROR"
                alert.headerText = "Uh, we can't login the user!"
                alert.contentText = "Name or password is incorrect"
                alert.showAndWait()
            }
        }

        val borderPane = BorderPane()
        borderPane.center = userAction
        val scene = Scene(borderPane, 800.0, 400.0)
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun registrationScene(primaryStage: Stage) {
        val userAction = createUserForm(primaryStage, "Register!") { username, password ->
            val newClientController = ClientController(username, password)
            if (newClientController.registerUser()) {
                clientController = newClientController
                bottomStatus.text = "Connection status: ${if (clientController!!.getConnectStatus()) "Connected" else "Disconnected"}"
                messengerScene(primaryStage)
            } else {
                val alert = Alert(Alert.AlertType.ERROR)
                alert.title = "ERROR"
                alert.headerText = "Uh, the user already exists. You cannot register user with this user name!"
                alert.contentText = "Try a different user name or try to login."
                alert.showAndWait()
            }
        }

        val borderPane = BorderPane()
        borderPane.center = userAction
        val scene = Scene(borderPane, 800.0, 400.0)
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun runClient(){
        try {

            while (!Thread.currentThread().isInterrupted){
                    sleep(1000)
                if(clientController!!.getConnectStatus()){
                    communicationWithServer()
                }else{
                    Platform.runLater {
                        clientController!!.startConnection()
                        bottomStatus.text="Connection status: ${if (clientController!!.getConnectStatus()) "Connected" else "Disconnected"}"
                    }
                }
            }
        }catch (e: InterruptedException) {
            println("Thread was interrupted during sleep")
        }
    }

    private fun loadMessagesForUser() {
        if(userList.selectionModel.selectedItem!=null){
            val selectedUser = userList.selectionModel.selectedItem
            messageList.items.clear()
            messageList.items.addAll(clientController!!.getAllMessageWith(selectedUser))
        }
        clientController!!.getAllConverClientNames().forEach { name ->
            if (!userList.items.contains(name)) {
                userList.items.add(name)
            }
        }
    }

    private fun communicationWithServer(){
            try {
                while (!Thread.currentThread().isInterrupted) {
                    if (this.clientController!!.isMessageFromServer()) {
                        when (this.clientController!!.processMessageFromServer()) {
                            "Bye" -> {
                                break
                            }
                            "Unfindable" -> {
                                Platform.runLater {
                                    val alert = Alert(AlertType.WARNING)
                                    alert.title = "WARNING!!!"
                                    alert.headerText = "Oops!"
                                    alert.contentText = "We couldn't find a user with that user name"
                                    alert.showAndWait()
                                }
                            }
                            "Response" -> {
                                Platform.runLater { loadMessagesForUser() }
                            }
                        }
                    }
                }
            }finally {
                clientController!!.closeConnection()
            }
    }

}

fun main() {
    Application.launch(ClientView::class.java)
}