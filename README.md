<h1><img src="https://github.com/jakegallon/recovery-tool/blob/master/src/res/icon.png" width="64" height="64" alt="logo"/> RecoveryTool</h1>
<p>RecoveryTool is an open source (GPL v3) file recovery software written in Java. It is capable of parsing NTFS and FAT32 file systems, detecting deleted files, and recovering their data.</p>
<p></p>
<h2>Features</h2>
<ul>
  <li><p><b>Low level byte reading</b>: Fast and efficient handling of reading, parsing and restoring bytes directly from a given drive or partition.</p></li>
  <li><div><b>NTFS parsing</b>:
    <ul> 
        <li>Reading of the boot sector for file system information.</li>
        <li>Reading of the MFT to obtain MFT records.</li>
        <li>Parsing of MFT record from its bytes to obtain fields necessary for file recovery.</li>
        <li>Parsing of MFT record attributes 0x10, 0x30, and 0x80. Parsing for resident and non-resident 0x80 attributes, allowing recovery from non-contiguous files of arbitrary length stored across the drive.</li>
    </ul>
  </div></li>
  <li><div><b>FAT32 parsing</b>:
    <ul> 
        <li>Reading of the boot sector for file system information.</li>
        <li>Recursive reading from the root directory to find all directories and files, saving only deleted files.</li>
        <li>Long filename handling in the case that a file's name is longer than 8 characters.</li>
    </ul>
  </div></li>
  <li><p><b>Multithreaded</b>: Utilizes multithreading to improve performance and reduce scanning, processing, and recovery times.</p></li>
  <li><p><b>File validation</b>: Apache Tika is used to compare a file's MIME data to its original record's extension to verify that the recovered file matches with the expected format.</p></li>
  <li><p><b>Logging</b>: Optional logging component to output information to the user as the drive is scanned and processed</p></li>
  <li><p><b>Progress Indicators</b>: Real-time progress bars to show information about the scanning, processing, and recovery of files.</p></li>
  <li><p><b>Filtering, sorting and searching</b>: Deleted files are displayed to the user in a table which is sortable, searchable, and has filters. The user selects which files they want to recover by clicking on the table row, represented by a checkbox in the first column.</p></li>
  <li><p><b>Custom error messages</b>: Custom error messages are displayed in several locations, such as when a user tries to put recovered files on the same drive that is being recovered from.</p></li>
</ul>
<h2>Credits</h2>
<p>Two third-party libraries are used in this project:</p>
<ul>
  <li><a href="https://github.com/apache/tika">Apache Tika</a> is used to analyze MIME data of recovered NTFS files, to check it matches with the original MFT record.</li>
  <li><a href="https://github.com/JFormDesigner/FlatLaf">FlatLAF</a> is used to provide a theme to the application's Swing components.</li>
</ul>
