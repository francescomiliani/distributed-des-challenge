package distributeddeschallenge.server;

import javafx.application.*; 
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.paint.*;
import javafx.event.*; 
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.text.Text;

/**
 *
 * @author Francesco Miliani & Gabriele Lagani
 * 
*/
class ConstantContainer {
	//Strings
	public final static String TITLE = "Distributed DES Challenge";
	public final static String INPUT_LABEL = "shell>";
	public final static String BUTTON_LABEL = "Enter";
	public final static String LABEL_STYLE = "-fx-font-size: 30px; -fx-text-fill: black; -fx-font-weight: bold;";
	public final static String TEXT_STYLE = "-fx-font-size: 14px; -fx-text-fill: black;";
	
	//Size
	public final static double STAGE_HEIGHT = 600;
	public final static double STAGE_WIDTH = 600;
	public final static double SCROLL_PANE_WIDTH = STAGE_WIDTH - 40;
	public final static double SCROLL_PANE_HEIGHT = STAGE_HEIGHT - 170;
	
	//Position in the Stage
	public final static double X_BOARD_POSITION = 20;
	public final static double Y_INPUT_OBJECTS_POSITION = 100;
	public final static double X_INPUT_TEXT = 70;
	public final static double Y_INPUT_TEXT = Y_INPUT_OBJECTS_POSITION;
	public final static double X_SCROLL_PANE = X_BOARD_POSITION;
	public final static double Y_SCROLL_PANE = 150;
	public final static double X_BUTTON = 250;
	public final static double Y_BUTTON = Y_INPUT_OBJECTS_POSITION;
	public final static double X_SHELL_LABEL = X_BOARD_POSITION;
	public final static double Y_SHELL_LABEL = Y_INPUT_OBJECTS_POSITION;
}

/**
 * Graphic User Interface to communicate with MasterServer by send itself several commands
 * Furthermore, launch MasterServerThread
 */
public class ServerGUI extends Application {  
  
    private Label la, shell_la;      
    private TextField input_tf;
	private Text result_tf;
    private Button bu;
	private ScrollPane sp;
	
	MasterServerThread server_thread;
    
	@Override
    public void start( Stage stage ) {    
   
        la = new Label( ConstantContainer.TITLE );
		shell_la = new Label( ConstantContainer.INPUT_LABEL );
        input_tf = new TextField( "help" );//So that it's immediatly call this command
		result_tf = new Text( "" );
		
        bu = new Button( ConstantContainer.BUTTON_LABEL );
		sp = new ScrollPane();
		sp.setContent( result_tf );
		sp.setFitToWidth(true);
		sp.setHbarPolicy( ScrollBarPolicy.NEVER );//do not show the horizontal scrool bar
        
        la.setLayoutX( ConstantContainer.X_BOARD_POSITION );  
        la.setStyle( ConstantContainer.LABEL_STYLE );

        shell_la.setLayoutX( ConstantContainer.X_SHELL_LABEL );  shell_la.setLayoutY( ConstantContainer.Y_SHELL_LABEL );//as input_tf
        shell_la.setStyle( ConstantContainer.TEXT_STYLE );		
        //input_tf.setMinWidth( 400 );
		
		sp.setPrefSize( ConstantContainer.SCROLL_PANE_WIDTH , ConstantContainer.SCROLL_PANE_HEIGHT );
		sp.setLayoutX( ConstantContainer.X_SCROLL_PANE ); sp.setLayoutY( ConstantContainer.Y_SCROLL_PANE  );
		
        input_tf.setLayoutX( ConstantContainer.X_INPUT_TEXT ); input_tf.setLayoutY( ConstantContainer.Y_INPUT_TEXT );
        input_tf.setStyle( ConstantContainer.TEXT_STYLE );
		
        result_tf.setStyle( ConstantContainer.TEXT_STYLE );
		result_tf.wrappingWidthProperty().bind( sp.widthProperty() );
		
        bu.setLayoutX( ConstantContainer.X_BUTTON ); bu.setLayoutY( ConstantContainer.Y_BUTTON ); 
        bu.setStyle( ConstantContainer.TEXT_STYLE );
         
        bu.setOnAction((ActionEvent ev)->{ 
			sendCommand();
		});

        Group root = new Group(la, shell_la, sp, input_tf, result_tf, bu);

        Scene scene = new Scene( root, ConstantContainer.STAGE_WIDTH, ConstantContainer.STAGE_HEIGHT, Color.WHITE );         
        stage.setTitle( ConstantContainer.TITLE );          
        stage.setScene(scene);
		
        stage.show(); 
        
		try {//Start MasterServer together with GUI
			server_thread = new MasterServerThread();
			server_thread.start();
		}catch( Exception e) {}

    }
	
    public static void main(String args[]) {
		launch(args);
	}
	
    private void sendCommand() {
		if( !server_thread.online ) {
			System.err.println("ERROR: Server is OFF...");
			result_tf.setText( "ERROR: Server is OFF...");
			//input_tf.setDisable(true);
			return;
		}
        String command = input_tf.getText();
		String result;
		input_tf.setText("");//Reset the input text field
		result_tf.setText( "");//Reset the result text field
				
		switch( command ) {
			case "help":
				result = "Command List: \n"+
						"- ciphertext: return the ciphertext\n" + 
						"- plaintext: return the plaintext if the key has been found\n" + 
						"- vol_num: return number of working voluteers\n" +
						"- vol_list: return a list of <VolunteerID, BlockID>\n"+
						"- state: return completion percentage of current state\n" + 
						"- close: switch off the server only the key has been found, otherwise not";
				break;
			case "ciphertext":
				result = server_thread.getCiphertext();
				break;
			case "plaintext":
				result = server_thread.getPlaintext();
				break;
			case "vol_num":
				result = server_thread.getVolunteerNumber();
				break;
			case "vol_list":
				result = server_thread.getVolunteerState();
				break;
			case "state":
				result = server_thread.getChallengeState();
				break;
			case "close":
				result = server_thread.switchOff();
				break;
			default:
				result = "Command not valid.";
				break;
		}
        result_tf.setText( result );
    }
}