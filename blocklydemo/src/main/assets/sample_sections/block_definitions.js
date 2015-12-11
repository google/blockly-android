'use strict';

// Define each block
//
//var simple_input_output_json = {"id": "simple_input_output",
//                              "message0": "test block %1",
//                              "args0": [
//                                {
//                                  "type": "input_value",
//                                  "name": "value"
//                                }
//                              ],
//                              "output": null,
//                              "tooltip": "",
//                              "helpUrl": "http://www.example.com/"};
//Blockly.Blocks['simple_input_output'] = {
//  init: function() {
//    this.jsonInit(simple_input_output_json);
//  }
//};
//
//var multiple_input_output_json = {"id": "multiple_input_output",
//                              "message0": "test block %1 %2",
//                              "args0": [
//                                {
//                                  "type": "input_value",
//                                  "name": "value_1"
//                                },
//                                {
//                                  "type": "input_value",
//                                  "name": "value_2"
//                                }
//                              ],
//                              "output": null,
//                              "tooltip": "",
//                              "helpUrl": "http://www.example.com/"};
//Blockly.Blocks['multiple_input_output'] = {
//  init: function() {
//    this.jsonInit(multiple_input_output_json);
//  }
//};
//
//var statement_no_input_json = {"id": "statement_no_input",
//                              "message0": "test block ",
//                              "args0": [],
//                              "previousStatement": null,
//                              "nextStatement": null,
//                              "tooltip": "",
//                              "helpUrl": "http://www.example.com/"};
//Blockly.Blocks['statement_no_input'] = {
//  init: function() {
//    this.jsonInit(statement_no_input_json);
//  }
//};
//
//var statement_value_input_json = {"id": "statement_value_input",
//                              "message0": "test block %1",
//                              "args0": [
//                                {
//                                  "type": "input_value",
//                                  "name": "value"
//                                }
//                              ],
//                              "previousStatement": null,
//                              "nextStatement": null,
//                              "tooltip": "",
//                              "helpUrl": "http://www.example.com/"};
//Blockly.Blocks['statement_value_input'] = {
//  init: function() {
//    this.jsonInit(statement_value_input_json);
//  }
//};
//
//var statement_multiple_value_input_json = {"id": "statement_multiple_value_input",
//                              "message0": "test block %1 %2",
//                              "args0": [
//                                {
//                                  "type": "input_value",
//                                  "name": "value_1"
//                                },
//                                {
//                                  "type": "input_value",
//                                  "name": "value_2"
//                                }
//                              ],
//                              "previousStatement": null,
//                              "nextStatement": null,
//                              "tooltip": "",
//                              "helpUrl": "http://www.example.com/"};
//Blockly.Blocks['statement_multiple_value_input'] = {
//  init: function() {
//    this.jsonInit(statement_multiple_value_input_json);
//  }
//};
//
//var statement_statement_input_json = {"id": "statement_statement_input",
//                              "message0": "test block %1",
//                              "args0": [
//                                {
//                                  "type": "input_statement",
//                                  "name": "statement input"
//                                }
//                              ],
//                              "previousStatement": null,
//                              "nextStatement": null,
//                              "tooltip": "",
//                              "helpUrl": "http://www.example.com/"};
//Blockly.Blocks['statement_statement_input'] = {
//  init: function() {
//    this.jsonInit(statement_statement_input_json);
//  }
//};
//
//var output_no_input_json = {"id": "output_no_input",
//                              "message0": "test block",
//                              "args0": [],
//                              "output": null,
//                              "tooltip": "",
//                              "helpUrl": "http://www.example.com/"};
//Blockly.Blocks['output_no_input'] = {
//  init: function() {
//    this.jsonInit(output_no_input_json);
//  }
//};
//
//var statement_no_next_json = {"id": "statement_no_next",
//                              "message0": "test block",
//                              "args0": [],
//                              "previousStatement": null,
//                              "tooltip": "",
//                              "helpUrl": "http://www.example.com/"};
//Blockly.Blocks['statement_no_next'] = {
//  init: function() {
//    this.jsonInit(statement_no_next_json);
//  }
//};

var mygetrequest = new XMLHttpRequest();
mygetrequest.onreadystatechange=function(){
 if (mygetrequest.readyState==4){
  if (mygetrequest.status==200 || window.location.href.indexOf("http")==-1){
   var jsondata=eval(mygetrequest.responseText) //retrieve result as an JavaScript object
   console.log(jsondata);
  }
  else{
   alert("An error has occured making the request");
  }
 }
};

mygetrequest.open("GET", "data:///android_asset/sample_sections/block_definitions.json");
mygetrequest.send();

//
//if(jsonArr) {
//  for (elem in jsonArr) {
//    Blockly.Blocks[elem.id] = {
//      init:function() {
//        this.jsonInit(elem);
//      }
//    }
//  }
//}
// Define each block's generated code

Blockly.JavaScript['simple_input_output'] = function(block) {
  return ['simple_input_output', Blockly.JavaScript.ORDER_ATOMIC];
};

Blockly.JavaScript['multiple_input_output'] = function(block) {
  return ['multiple_input_output', Blockly.JavaScript.ORDER_ATOMIC];
};

Blockly.JavaScript['statement_no_input'] = function(block) {
  return 'do something;\n';
};

Blockly.JavaScript['statement_value_input'] = function(block) {
  return 'statement_value_input';
};

Blockly.JavaScript['statement_multiple_value_input'] = function(block) {
  return 'statement_multiple_value_input';
};

Blockly.JavaScript['statement_statement_input'] = function(block) {
  return 'statement_statement_input';
};

Blockly.JavaScript['output_no_input'] = function(block) {
  return ['output_no_input', Blockly.JavaScript.ORDER_ATOMIC];
};

Blockly.JavaScript['statement_no_next'] = function(block) {
  return 'statement_no_next';
};