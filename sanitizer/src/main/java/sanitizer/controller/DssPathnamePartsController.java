package sanitizer.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class DssPathnamePartsController {

    @FXML
    TextField partA, partB, partC, partD, partE, partF;

    @FXML
    void initialize(){
        partA.setText("*");
        partB.setText("*");
        partC.setText("*");
        partD.setText("*");
        partE.setText("*");
        partF.setText("*");
    }

    String getPartA(){
        return partA.getText();
    }

    String getPartB(){
        return partB.getText();
    }

    String getPartC(){
        return partC.getText();
    }

    String getPartD(){
        return partD.getText();
    }

    String getPartE(){
        return partE.getText();
    }

    String getPartF(){
        return partF.getText();
    }

}
