// Convert "HexHexHexHex" to [byte, byte, byte]
function convert_formated_hex_to_bytes(hex_str) {
  return hex_str.replace(/[^0-9a-fA-F]+/, "") // remove invalid
    .split(/([0-9a-fA-F]{2})/g) // split into pairs
    .filter(w => w.length > 0) // remove empty items
    .map(w => parseInt(w, 16)); // convert back to bytes
}

// Convert [byte, byte, byte] to "HexHexHexHex"
function convert_to_formated_hex(byte_arr) {
  return byte_arr.map(q => (q < 0) ? q + 256 : q) // convert to [0..256]
    .map(q => q.toString(16)) // map to hex
    .map(w => w.length == 1 ? "0" + w : w) // add leading "0"
    .reduce((a, w) => a + w); // join to a single string
}

function lzma_benchmark(input) {
  let start_time = (new Date).getTime();

  LZMA.compress(input, 9, (comp_result) => {
      let compData = convert_to_formated_hex(comp_result);
      let compSize = compData.length;
      let comp_done = (new Date).getTime();

      LZMA.decompress(convert_formated_hex_to_bytes(compData), (result) => {
          let decomp_done = (new Date).getTime();

          let compRatio = `${(compSize / input.length).toFixed(3)}%`;
          let resultStr = `Total: ${decomp_done - start_time} ms\n`;
          resultStr += `Compress: ${comp_done - start_time} ms (${compSize} bytes) @ ${compRatio}\n`;
          resultStr += `Decompress: ${decomp_done - comp_done} ms (${input.length} bytes)\n`;
          resultStr += `Compare: ${input == result}`;
          console.log(resultStr);
        }, ()=>{});
    }, ()=>{});
}
