/**
 * P1_Input is the package containing all the input modules, reading documents from various forms of data input,
 * and producing a standardised corpus JSON file for the rest of the pipeline.
 * <br>
 * The input modules are:<br>
 *  - {@link P1_Input.CSVInput} which reads documents from a CSV file and allows to save several data fields;<br>
 *  - {@link P1_Input.TXTInput} which reads documents from a directory of TXT files or a single TXT file;<br>
 *  - {@link P1_Input.PDFInput} which reads documents from a directory of PDF files;<br>
 *  - {@link P1_Input.GTRInput} which reads grant IDs from a CSV file (with other fields) and crawls Gateway to Research (GtR) to find additional info.
 *
 * @deprecated
 * Replaced by input package in version 3
 */
@Deprecated
package P1_Input;