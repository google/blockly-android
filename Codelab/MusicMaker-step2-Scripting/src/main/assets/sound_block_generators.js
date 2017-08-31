'use strict';

// Generators for blocks defined in `sound_blocks.json`.
Blockly.JavaScript['play_sound'] = function(block) {
  var value = '\'' + block.getFieldValue('VALUE') + '\'';
  return 'MusicMaker.playSound(' + value + ');\n';
};
