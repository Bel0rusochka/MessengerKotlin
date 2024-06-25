import Client.Client
import Server.ClientMessageModel
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


class MainApp: Application() {
    private val client = Client("Anton","229")
    private val messageList = ListView<String>()
    private val userList = ListView<String>()

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Messenger App"
        userList.items.addAll(client.getAllConverClientNames())

        val messageInput = TextField()
        val sendButton = Button("Send")
        val messageInputBox = HBox(messageInput, sendButton)
        messageInputBox.alignment = Pos.CENTER
        messageInputBox.isVisible = false
        HBox.setHgrow(messageInput, Priority.ALWAYS)


        val userNameInput = TextField()
        val addUserButton= Button("Add")
        val userNameInputBox = HBox(userNameInput, addUserButton)
        userNameInputBox.alignment = Pos.CENTER
        HBox.setHgrow(userNameInput, Priority.ALWAYS)


        val userNameLabel = Label("Messages")


        userList.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            messageInputBox.isVisible = newValue != null
            userNameLabel.text=newValue
            this.loadMessagesForUser()
        }


        addUserButton.setOnAction {
            val userName = userNameInput.text
            if (!userList.items.contains(userName)) {
                userList.items.add(userName)
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
        messagesPane.padding = Insets(10.0)
        VBox.setVgrow(messageList, Priority.ALWAYS)

        val usersPane = VBox()
        usersPane.children.addAll(Label("Users"),userNameInputBox, userList)
        usersPane.padding = Insets(10.0)
        VBox.setVgrow(userList, Priority.ALWAYS)

        val borderPane = BorderPane()
        borderPane.left = usersPane
        borderPane.center = messagesPane


        val scene = Scene(borderPane, 800.0, 400.0)
        primaryStage.scene = scene


        primaryStage.show()

        Thread{
            this.communicationWithServer()
        }.start()
    }

    private fun loadMessagesForUser() {
        Thread {
            val selectedUser = userList.selectionModel.selectedItem
            val userMessages = mutableListOf<String>()

            userMessages.addAll( client.getAllMessageWith(selectedUser))


            Platform.runLater {
                messageList.items.clear()
                messageList.items.addAll(userMessages)

                client.getAllConverClientNames().forEach { name ->
                    if (!userList.items.contains(name)) {
                        userList.items.add(name)
                    }
                }
            }
        }.start()
    }

    private fun communicationWithServer(){
        try {
            while (true){
                if(this.client.isMessageFromServer()) {
                    this.client.processMessageFromServer()
                    Platform.runLater {
                        if(userList.selectionModel.selectedItem!=null)
                        {
                            loadMessagesForUser()
                        }

                    }
                }
            }
        } finally {
            this.client.closeConnection()
        }
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}
