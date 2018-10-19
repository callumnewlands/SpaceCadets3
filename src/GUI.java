import javafx.application.Application;

import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GUI extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private static final Pattern HIGHLIGHTING_PATTERN = Pattern.compile(
            "(?<KEYWORD>" + Interpreter.KEYWORD_REG_EX + ")"
                    + "|(?<SEMICOLON>" + Interpreter.SEMICOLON_REG_EX + ")"
                    + "|(?<COMMENT>" + Interpreter.COMMENT_REG_EX + ")");



    public void show() { launch(); } // launch GUI

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Bare Bones IDE");


        // Define Code Area
        CodeArea codeEditor = new CodeArea();
        codeEditor.setMaxWidth(WIDTH / 2);
        codeEditor.setMinWidth(WIDTH / 2);
        codeEditor.setMinHeight(HEIGHT);
        codeEditor.setMaxHeight(HEIGHT);
        codeEditor.setParagraphGraphicFactory(LineNumberFactory.get(codeEditor));  // Add line numbers
        loadFileIntoEditor(new File("./resources/multiply.txt"), codeEditor);
        codeEditor.setOnKeyTyped(new EventHandler<>() {
            @Override
            public void handle(KeyEvent event) {
                codeEditor.setStyleSpans(0, computeHighlighting(codeEditor.getText()));
            }
        });

        ScrollPane codeScroll = new ScrollPane();
        codeScroll.setContent(codeEditor);
        codeScroll.setMinWidth(codeEditor.getMinWidth());
        codeScroll.setMaxWidth(codeEditor.getMaxWidth());
        codeScroll.setMinHeight(HEIGHT);
        codeScroll.setMaxHeight(HEIGHT);
        codeScroll.setPrefSize(codeEditor.getWidth(), HEIGHT);

        // Define Output Text
        Text txtOutput = new Text();
        txtOutput.setText("OUTPUT:");

        ScrollPane outputScroll = new ScrollPane();
        outputScroll.setContent(txtOutput);
        outputScroll.setMinHeight(HEIGHT / 2);
        outputScroll.setMaxHeight(HEIGHT / 2);
        outputScroll.setPrefWidth(WIDTH / 4);

        // Define Run Button
        Button btnRun = new Button();
        btnRun.setText("Run");
        btnRun.setOnAction(new EventHandler<>() {
            @Override public void handle(ActionEvent event) {

                try
                {
                    Interpreter interpreter = new Interpreter(codeEditor.getText(), txtOutput);
                    interpreter.execute();
                }
                catch (InterpreterException e)
                {
                    System.out.println(e.getMessage());
                }
                outputScroll.setVvalue(1.0); // Scroll to the bottom of the output window
            }
        });

        // Define Load Button
        Button btnLoad = new Button();
        btnLoad.setText("Load File");
        btnLoad.setOnAction(new EventHandler<>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser chooser = new FileChooser();
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt"));
                chooser.setTitle("Open Text File");
                File file = chooser.showOpenDialog(primaryStage);
                loadFileIntoEditor(file, codeEditor);  // Load the specified file into the editor and set syntax highlighting
            }
        });



        // Define right column layout
        VBox rightColumn = new VBox();
        rightColumn.setSpacing(10);
        rightColumn.getChildren().addAll(btnRun, btnLoad, outputScroll);  // Add components to r.hand column

        // Define horizontal layout
        HBox hbox = new HBox();
        hbox.setSpacing(10);
        hbox.getChildren().addAll(codeScroll, rightColumn);  // Add code editor on left and right column on right

        // Create window
        Scene scene = new Scene(hbox, WIDTH, HEIGHT);
        scene.getStylesheets().add("syntaxHighlighting.css");  // Load the syntax highlighting stylesheet
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadFileIntoEditor(File file, CodeArea codeEditor) {
        if (file != null)
        {
            String code = "";
            try (BufferedReader reader = new BufferedReader(new FileReader(file)))
            {
                String line;
                while ((line = reader.readLine()) != null) { // For each line in the file
                    Pattern commentPattern = Pattern.compile(Interpreter.COMMENT_REG_EX);
                    Matcher commentMatcher = commentPattern.matcher(line);
                    if (!commentMatcher.matches())  // If line is not a comment
                    {
                        code += line.toLowerCase() + "\n";
                    }
                    else
                    {
                        code += line + "\n";
                    }
                }
                codeEditor.replaceText(code);
                codeEditor.setStyleSpans(0, computeHighlighting(codeEditor.getText())); // Set highlighting from the fist character of the code
            }
            catch (IOException e)
            {
                codeEditor.replaceText("CANNOT LOAD FILE");
            }
        }
    }


    private static StyleSpans<Collection<String>> computeHighlighting(String code) {
        Matcher matcher = HIGHLIGHTING_PATTERN.matcher(code);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>(); // Used to build a list of style tuples (StyleSpans)

        while(matcher.find()) {
            // Sets the style name for the matched term (i.e. if it matches the keyword pattern, styleClass="keyword") uses the ternary comparison operator
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("SEMICOLON") != null ? "semicolon" :
                                    matcher.group("COMMENT") != null ? "comment" :
                                            null; // Will only be null if
            assert styleClass != null;

            // Add the unformatted text between the end of the last keyword and the start of the next one with an empty formatting
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            // Add the matched term to the builder with the specified style
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            // Set the position of the end of the keyword just added
            lastKwEnd = matcher.end();
        }
        // Add the remainder of the text with no formatting
        spansBuilder.add(Collections.emptyList(), code.length() - lastKwEnd);
        return spansBuilder.create(); // Build and return the list of StyleSpans
    }

}
