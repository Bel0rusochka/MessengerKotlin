import Client.Client
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
import java.lang.Thread.sleep


class MainApp: Application() {
    private val client = Client("Anton","229")
    private val messageList = ListView<String>()
    private val userList = ListView<String>()
    private val bottomStatus = Label("Connection status: ${if (client.getConnectStatus()) "Connected" else "Disconnected"}")

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Messenger App"
        val userNameLabel = Label("Messages")
        userList.items.addAll(client.getAllConverClientNames())

        val messageInput = TextField()
        val sendButton = Button("Send")
        val messageInputBox = HBox(messageInput, sendButton)
        messageInputBox.alignment = Pos.CENTER
        messageInputBox.isVisible = false
        HBox.setHgrow(messageInput, Priority.ALWAYS)


        val userNameInput = TextField()
        val addUserButton= Button("Add")
        val deleteUserButton = Button("Delete")
        deleteUserButton.style ="-fx-background-color: #FF4040; "
        val userNameInputBox = HBox(userNameInput, addUserButton,deleteUserButton)
        userNameInputBox.alignment = Pos.CENTER
        HBox.setHgrow(userNameInput, Priority.ALWAYS)


        userList.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            messageInputBox.isVisible = newValue != null
            if (userList.selectionModel.selectedItem!= null) userNameLabel.text=newValue
            this.loadMessagesForUser()
        }

        addUserButton.setOnAction {
            val userName = userNameInput.text.trim()
            if(userName!="" && !userList.items.contains(userName)) {
                userList.items.add(userName)
            }
            userNameInput.clear()
        }

        deleteUserButton.setOnAction {
            val selectedUser = userList.selectionModel.selectedItem
            if (selectedUser != null) {
                this.client.deleteAllMessagesWithConvClient(selectedUser)
                this.messageList.items.clear()
                userNameLabel.text = "Messages"
                userList.items.remove(selectedUser)
            }
        }

        sendButton.setOnAction {
            val selectedUser = userList.selectionModel.selectedItem
            val messageText = messageInput.text
            if (selectedUser != null) {
                this.client.sendMessage(TypeMessage.SEND, messageText, selectedUser)
                this.loadMessagesForUser()
                messageInput.clear()
            }
        }

        val messagesPane = VBox()
        messagesPane.children.addAll(userNameLabel, messageList, messageInputBox)
        messagesPane.padding = Insets(5.0, 10.0,5.0, 10.0)
        VBox.setVgrow(messageList, Priority.ALWAYS)

        val usersPane = VBox()
        usersPane.children.addAll(Label("Users"),userNameInputBox, userList)
        usersPane.padding = Insets(5.0, 10.0,5.0, 10.0)
        VBox.setVgrow(userList, Priority.ALWAYS)

        val lb = Label("My name is: ${client.getName()}")
        lb.padding = Insets(0.0, 10.0, 0.0, 10.0)

        val borderPane = BorderPane()
        borderPane.top = lb
        borderPane.left = usersPane
        borderPane.center = messagesPane

        bottomStatus.padding = Insets(0.0, 10.0, 5.0, 10.0)
        borderPane.bottom = bottomStatus

        val scene = Scene(borderPane, 800.0, 400.0)
        primaryStage.scene = scene

        primaryStage.show()
        val thread2 = Thread{ this.runClient() }
        thread2.start()

        primaryStage.setOnCloseRequest {

                thread2.interrupt()


            client.closeDB()
        }
    }

    private fun runClient(){
        try {
            while (!Thread.currentThread().isInterrupted){
                    sleep(1000)
                if(client.getConnectStatus()){
                    communicationWithServer()
                }else{
                    Platform.runLater {
                        client.initConnection()
                        bottomStatus.text="Connection status: ${if (client.getConnectStatus()) "Connected" else "Disconnected"}"
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
            messageList.items.addAll(client.getAllMessageWith(selectedUser))
        }
        client.getAllConverClientNames().forEach { name ->
            if (!userList.items.contains(name)) {
                userList.items.add(name)
            }
        }
    }

    private fun communicationWithServer(){
            try {
                while (!Thread.currentThread().isInterrupted) {
                    if (this.client.isMessageFromServer()) {
                        when (this.client.processMessageFromServer()) {
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
                this.client.closeConnection()
            }
    }

}

fun main() {
    Application.launch(MainApp::class.java)
}
