'use strict';

var jsonArr = [];
var mygetrequest = new XMLHttpRequest();

mygetrequest.onreadystatechange=function(){
 if (mygetrequest.readyState==4){
  if (mygetrequest.status==200 || window.location.href.indexOf("http")==-1){
  //console.log(mygetrequest.responseText);
   jsonArr=eval(mygetrequest.responseText) //retrieve result as an JavaScript object
   console.log(jsonArr);
  }
  else{
   alert("An error has occured making the request");
  }
 }
};

mygetrequest.open("GET", "http://android_asset/sample_sections/block_definitions.json");
mygetrequest.send();


console.log(jsonArr);
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