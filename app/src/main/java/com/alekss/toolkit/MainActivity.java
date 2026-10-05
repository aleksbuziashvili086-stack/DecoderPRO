package com.alekss.toolkit;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import java.security.MessageDigest;
public class MainActivity extends Activity {

    private static final int PICK_FILE = 1001;
    private static final int PICK_FOLDER = 1002;

    private LinearLayout explorer;
    private EditText editorText;
    private TextView editorTitle;
    private TextView lineNumbers;
    private File openedLocalFile;
    private Uri openedSafFile;
    private boolean safEditing = false;

    private File projectRoot;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildIDE();
    }

    private TextView label(String text, float size) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        v.setPadding(12, 9, 12, 9);
        return v;
    }

    private void buildIDE() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18,18,18));

        // TOP BAR
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(Color.rgb(32,32,32));

        TextView title = label("DecoderPRO", 19);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        top.addView(
                title,
                new LinearLayout.LayoutParams(0, 58, 1)
        );

        Button open = new Button(this);
        open.setText("OPEN");
        open.setOnClickListener(v -> showOpenMenu());

        top.addView(open,
                new LinearLayout.LayoutParams(110, 58));

        Button save = new Button(this);
        save.setText("SAVE");
        save.setOnClickListener(v -> saveCurrentFile());

        top.addView(save,
                new LinearLayout.LayoutParams(105, 58));

        root.addView(top);

        // WORKSPACE
        LinearLayout workspace = new LinearLayout(this);
        workspace.setOrientation(LinearLayout.HORIZONTAL);

        // PROJECT EXPLORER
        ScrollView explorerScroll = new ScrollView(this);

        explorer = new LinearLayout(this);
        explorer.setOrientation(LinearLayout.VERTICAL);
        explorer.setBackgroundColor(Color.rgb(24,24,24));

        explorer.addView(
                label("PROJECT EXPLORER", 12)
        );

        explorer.addView(
                label("📁 Open ZIP or Folder", 14)
        );

        explorerScroll.addView(explorer);

        workspace.addView(
                explorerScroll,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0.36f
                )
        );

        // EDITOR
        LinearLayout editorArea = new LinearLayout(this);
        editorArea.setOrientation(LinearLayout.VERTICAL);
        editorArea.setBackgroundColor(Color.rgb(12,12,12));

        editorTitle = label(
                "NO FILE OPEN",
                12
        );

        editorTitle.setBackgroundColor(
                Color.rgb(30,30,30)
        );

        editorArea.addView(editorTitle);

        LinearLayout codeArea =
                new LinearLayout(this);

        codeArea.setOrientation(
                LinearLayout.HORIZONTAL
        );

        lineNumbers = new TextView(this);
        lineNumbers.setText("1");
        lineNumbers.setTextColor(
                Color.rgb(130,130,130)
        );
        lineNumbers.setTextSize(13);
        lineNumbers.setGravity(
                Gravity.TOP | Gravity.RIGHT
        );
        lineNumbers.setTypeface(
                Typeface.MONOSPACE
        );
        lineNumbers.setPadding(
                8, 8, 10, 8
        );
        lineNumbers.setBackgroundColor(
                Color.rgb(22,22,22)
        );

        codeArea.addView(
                lineNumbers,
                new LinearLayout.LayoutParams(
                        45,
                        LinearLayout.LayoutParams.MATCH_PARENT
                )
        );

        editorText = new EditText(this);

        editorText.setTextColor(Color.WHITE);
        editorText.setTextSize(13);
        editorText.setGravity(Gravity.TOP);
        editorText.setTypeface(
                Typeface.MONOSPACE
        );
        editorText.setBackgroundColor(
                Color.rgb(12,12,12)
        );
        editorText.setPadding(
                8, 8, 8, 8
        );
        editorText.setSingleLine(false);
        editorText.setHorizontallyScrolling(true);
        editorText.setTextIsSelectable(true);

        editorText.addTextChangedListener(
                new android.text.TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        updateLineNumbers();
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable e) {
                    }
                }
        );

        codeArea.addView(
                editorText,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                )
        );

        ScrollView codeScroll =
                new ScrollView(this);

        codeScroll.addView(codeArea);

        editorArea.addView(
                codeScroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        workspace.addView(
                editorArea,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0.64f
                )
        );

        root.addView(
                workspace,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        // BOTTOM TOOLBAR
        LinearLayout bottom =
                new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(8, 6, 8, 6);
        bottom.setMinimumHeight(dp(60));

        bottom.setBackgroundColor(
                Color.rgb(30,30,30)
        );

        Button search = new Button(this);
        search.setText("SEARCH");
        search.setOnClickListener(
                v -> showSearch()
        );

        bottom.addView(
                search,
                new LinearLayout.LayoutParams(
                        0, dp(48), 1f
                )
        );

        Button problems = new Button(this);
        problems.setText("PROBLEMS");
        problems.setOnClickListener(
                v -> showInfo(
                        "Problems\n\n" +
                        "Project diagnostics module."
                )
        );

        bottom.addView(
                problems,
                new LinearLayout.LayoutParams(
                        0, dp(48), 1f
                )
        );

        Button logs = new Button(this);
        logs.setText("LOGS");
        logs.setOnClickListener(
                v -> showInfo(
                        "Logs\n\n" +
                        "DecoderPRO activity log."
                )
        );

        bottom.addView(
                logs,
                new LinearLayout.LayoutParams(
                        0, dp(48), 1f
                )
        );

        root.addView(bottom);

        setContentView(root);
    }

    // =========================
    // OPEN MENU
    // =========================

    private void showOpenMenu() {

        String[] options = {
                "Import ZIP",
                "Open Folder"
        };

        new android.app.AlertDialog.Builder(this)
                .setTitle("Open Project")
                .setItems(
                        options,
                        (d, which) -> {
                            if (which == 0)
                                pickZip();
                            else
                                pickFolder();
                        }
                )
                .show();
    }

    private void pickZip() {

        Intent i =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        i.setType("*/*");

        i.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        startActivityForResult(
                i,
                PICK_FILE
        );
    }

    private void pickFolder() {

        Intent i =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT_TREE
                );

        i.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                i,
                PICK_FOLDER
        );
    }

    // =========================
    // RESULT
    // =========================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (resultCode != RESULT_OK ||
                data == null)
            return;

        Uri uri = data.getData();

        if (uri == null)
            return;

        if (requestCode == PICK_FILE) {

            importZip(uri);

        } else if (requestCode == PICK_FOLDER) {

            openFolder(uri);
        }
    }

    // =========================
    // ZIP
    // =========================

    private void importZip(Uri uri) {

        try {

            File root =
                    new File(
                            getCacheDir(),
                            "decoder_project"
                    );

            deleteRecursive(root);

            if (!root.mkdirs() &&
                    !root.exists()) {

                throw new IOException(
                        "Cannot create project"
                );
            }

            InputStream input =
                    getContentResolver()
                            .openInputStream(uri);

            if (input == null)
                throw new IOException(
                        "Cannot read ZIP"
                );

            ZipInputStream zip =
                    new ZipInputStream(
                            new BufferedInputStream(
                                    input
                            )
                    );

            byte[] buffer =
                    new byte[8192];

            ZipEntry entry;

            String rootPath =
                    root.getCanonicalPath();

            while ((entry =
                    zip.getNextEntry()) != null) {

                File out =
                        new File(
                                root,
                                entry.getName()
                        );

                String outPath =
                        out.getCanonicalPath();

                if (!outPath.equals(rootPath) &&
                        !outPath.startsWith(
                                rootPath +
                                File.separator
                        )) {

                    zip.closeEntry();
                    continue;
                }

                if (entry.isDirectory()) {

                    out.mkdirs();

                } else {

                    File parent =
                            out.getParentFile();

                    if (parent != null)
                        parent.mkdirs();

                    FileOutputStream fos =
                            new FileOutputStream(out);

                    int len;

                    while ((len =
                            zip.read(buffer)) > 0) {

                        fos.write(
                                buffer,
                                0,
                                len
                        );
                    }

                    fos.close();
                }

                zip.closeEntry();
            }

            zip.close();

            projectRoot = root;

            openedLocalFile = null;
            openedSafFile = null;
            safEditing = false;

            showProject();

            Toast.makeText(
                    this,
                    "ZIP imported",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            showError(
                    "ZIP import failed:\n\n" +
                    e.getMessage()
            );
        }
    }

    // =========================
    // FOLDER
    // =========================

    private void openFolder(Uri treeUri) {

        try {

            getContentResolver()
                    .takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

        } catch (Exception ignored) {
        }

        openedLocalFile = null;
        openedSafFile = null;
        safEditing = true;

        explorer.removeAllViews();

        explorer.addView(
                label(
                        "PROJECT EXPLORER",
                        12
                )
        );

        explorer.addView(
                label(
                        "📁 SELECTED FOLDER",
                        14
                )
        );

        addSafChildren(
                treeUri,
                treeUri,
                1
        );

        editorTitle.setText(
                "FOLDER PROJECT"
        );

        editorText.setText(
                "Folder opened.\n\n" +
                "Select a source/config file."
        );
    }

    // =========================
    // SAF TREE
    // =========================

    private void addSafChildren(
            Uri treeUri,
            Uri directoryUri,
            int depth) {

        String documentId =
                android.provider.DocumentsContract
                        .getTreeDocumentId(treeUri);

        if (directoryUri != treeUri) {

            documentId =
                    android.provider.DocumentsContract
                            .getDocumentId(
                                    directoryUri
                            );
        }

        Uri childrenUri =
                android.provider.DocumentsContract
                        .buildChildDocumentsUriUsingTree(
                                treeUri,
                                documentId
                        );

        android.database.Cursor cursor =
                getContentResolver()
                        .query(
                                childrenUri,
                                new String[]{
                                        android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                        android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                        android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
                                },
                                null,
                                null,
                                android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME
                        );

        if (cursor == null)
            return;

        try {

            int idIndex =
                    cursor.getColumnIndex(
                            android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID
                    );

            int nameIndex =
                    cursor.getColumnIndex(
                            android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    );

            int mimeIndex =
                    cursor.getColumnIndex(
                            android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
                    );

            while (cursor.moveToNext()) {

                String id =
                        cursor.getString(idIndex);

                String name =
                        cursor.getString(nameIndex);

                String mime =
                        cursor.getString(mimeIndex);

                Uri child =
                        android.provider.DocumentsContract
                                .buildDocumentUriUsingTree(
                                        treeUri,
                                        id
                                );

                boolean directory =
                        "vnd.android.document/directory"
                                .equals(mime);

                TextView item =
                        label(
                                getPrefix(depth) +
                                (directory
                                        ? "📁 "
                                        : iconName(name)) +
                                name,
                                14
                        );

                explorer.addView(item);

                if (directory) {

                    item.setTypeface(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                    );

                    addSafChildren(
                            treeUri,
                            child,
                            depth + 1
                    );

                } else {

                    item.setOnClickListener(
                            v -> openSafFile(
                                    child,
                                    name
                            )
                    );
                }
            }

        } finally {

            cursor.close();
        }
    }

    // =========================
    // OPEN SAF FILE
    // =========================

    private void openSafFile(
            Uri uri,
            String name) {

        if (!isTextName(name)) {

            editorTitle.setText(name);

            editorText.setText(
                    "BINARY FILE\n\n" +
                    "Name: " + name +
                    "\n\n" +
                    "HEX VIEWER WILL HANDLE THIS FILE."
            );

            return;
        }

        try {

            InputStream input =
                    getContentResolver()
                            .openInputStream(uri);

            if (input == null)
                throw new IOException(
                        "Cannot open file"
                );

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    "UTF-8"
                            )
                    );

            StringBuilder content =
                    new StringBuilder();

            String line;

            while ((line =
                    reader.readLine()) != null) {

                content.append(line)
                        .append('\n');
            }

            reader.close();

            editorTitle.setText(name);

            editorText.setText(
                    content.toString()
            );

            openedSafFile = uri;
            openedLocalFile = null;
            safEditing = true;

            updateLineNumbers();

        } catch (Exception e) {

            showError(
                    "Cannot open file:\n\n" +
                    e.getMessage()
            );
        }
    }

    // =========================
    // LOCAL TREE
    // =========================

    private void showProject() {

        explorer.removeAllViews();

        explorer.addView(
                label(
                        "PROJECT EXPLORER",
                        12
                )
        );

        explorer.addView(
                label(
                        "📁 " +
                        projectRoot.getName(),
                        15
                )
        );

        addLocalDirectory(
                projectRoot,
                1
        );
    }

    private void addLocalDirectory(
            File dir,
            int depth) {

        File[] files =
                dir.listFiles();

        if (files == null)
            return;

        Arrays.sort(
                files,
                (a,b) -> {

                    if (a.isDirectory() &&
                            !b.isDirectory())
                        return -1;

                    if (!a.isDirectory() &&
                            b.isDirectory())
                        return 1;

                    return a.getName()
                            .compareToIgnoreCase(
                                    b.getName()
                            );
                }
        );

        for (File file : files) {

            TextView item =
                    label(
                            getPrefix(depth) +
                            (file.isDirectory()
                                    ? "📁 "
                                    : icon(file)) +
                            file.getName(),
                            14
                    );

            explorer.addView(item);

            if (file.isDirectory()) {

                item.setTypeface(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                );

                addLocalDirectory(
                        file,
                        depth + 1
                );

            } else {

                item.setOnClickListener(
                        v -> openLocalFile(file)
                );
            }
        }
    }

    // =========================
    // LOCAL FILE
    // =========================

    private void openLocalFile(File file) {
        if (file != null && !isTextName(file.getName())) {
            openBinaryAnalyzer(file);
            return;
        }



        if (!isTextFile(file)) {

            editorTitle.setText(
                    file.getName()
            );

            editorText.setText(
                    "BINARY FILE\n\n" +
                    "Name: " +
                    file.getName() +
                    "\nSize: " +
                    file.length() +
                    " bytes\n\n" +
                    "HEX VIEWER WILL HANDLE THIS FILE."
            );

            return;
        }

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(file),
                                    "UTF-8"
                            )
                    );

            StringBuilder content =
                    new StringBuilder();

            String line;

            while ((line =
                    reader.readLine()) != null) {

                content.append(line)
                        .append('\n');
            }

            reader.close();

            editorTitle.setText(
                    file.getAbsolutePath()
            );

            editorText.setText(
                    content.toString()
            );

            openedLocalFile = file;
            openedSafFile = null;
            safEditing = false;

            updateLineNumbers();

        } catch (Exception e) {

            showError(
                    "Cannot open file:\n\n" +
                    e.getMessage()
            );
        }
    }

    // =========================
    // SAVE
    // =========================

    private void saveCurrentFile() {

        String content =
                editorText.getText().toString();

        try {

            if (openedLocalFile != null) {

                FileOutputStream out =
                        new FileOutputStream(
                                openedLocalFile
                        );

                out.write(
                        content.getBytes("UTF-8")
                );

                out.close();

                Toast.makeText(
                        this,
                        "Saved",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (openedSafFile != null) {

                OutputStream out =
                        getContentResolver()
                                .openOutputStream(
                                        openedSafFile,
                                        "wt"
                                );

                if (out == null)
                    throw new IOException(
                            "Cannot write file"
                    );

                out.write(
                        content.getBytes("UTF-8")
                );

                out.close();

                Toast.makeText(
                        this,
                        "Saved",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Toast.makeText(
                    this,
                    "No file open",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            showError(
                    "Save failed:\n\n" +
                    e.getMessage()
            );
        }
    }

    // =========================
    // SEARCH
    // =========================

    private void showSearch() {

        final EditText input =
                new EditText(this);

        input.setHint(
                "Search in current file"
        );

        input.setSingleLine(true);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Search")
                .setView(input)
                .setPositiveButton(
                        "Find",
                        (d, which) -> {

                            String query =
                                    input.getText()
                                            .toString();

                            findText(query);
                        }
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    private void findText(String query) {
        if (projectRoot != null && projectRoot.exists()) {
            searchWholeProject(query);
            return;
        }



        if (query == null ||
                query.isEmpty())
            return;

        String content =
                editorText.getText().toString();

        int position =
                content.indexOf(query);

        if (position < 0) {

            Toast.makeText(
                    this,
                    "Not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        editorText.requestFocus();

        editorText.setSelection(
                position,
                position + query.length()
        );

        InputMethodManager imm =
                (InputMethodManager)
                        getSystemService(
                                Context.INPUT_METHOD_SERVICE
                        );

        if (imm != null) {

            imm.showSoftInput(
                    editorText,
                    InputMethodManager.SHOW_IMPLICIT
            );
        }
    }

    // =========================
    // LINE NUMBERS
    // =========================

    private void updateLineNumbers() {

        String text =
                editorText.getText().toString();

        int lines = 1;

        for (int i = 0;
             i < text.length();
             i++) {

            if (text.charAt(i) == '\n')
                lines++;
        }

        StringBuilder nums =
                new StringBuilder();

        for (int i = 1; i <= lines; i++) {

            nums.append(i)
                    .append('\n');
        }

        lineNumbers.setText(
                nums.toString()
        );
    }

    // =========================
    // HELPERS
    // =========================

    private boolean isTextFile(File f) {
        return isTextName(f.getName());
    }

    private boolean isTextName(String name) {

        String n =
                name.toLowerCase();

        String[] ext = {

                ".java",
                ".kt",
                ".kts",

                ".xml",
                ".json",

                ".gradle",
                ".properties",

                ".txt",
                ".md",

                ".c",
                ".h",
                ".cc",
                ".cpp",
                ".hpp",

                ".cs",
                ".go",
                ".rs",

                ".py",
                ".rb",
                ".php",

                ".js",
                ".ts",
                ".jsx",
                ".tsx",

                ".html",
                ".htm",
                ".css",

                ".sh",
                ".bat",

                ".yaml",
                ".yml",

                ".ini",
                ".cfg",
                ".conf",

                ".cmake",
                ".pro",

                ".toml",

                ".gitignore",
                ".gitattributes"
        };

        for (String e : ext) {

            if (n.endsWith(e))
                return true;
        }

        return false;
    }

    private String getPrefix(int depth) {

        StringBuilder s =
                new StringBuilder();

        for (int i = 0; i < depth; i++)
            s.append("    ");

        return s.toString();
    }

    private String icon(File f) {
        return iconName(f.getName());
    }

    private String iconName(String name) {

        String n =
                name.toLowerCase();

        if (n.endsWith(".java") ||
                n.endsWith(".kt"))
            return "☕ ";

        if (n.endsWith(".xml"))
            return "🧩 ";

        if (n.endsWith(".json"))
            return "{} ";

        if (n.endsWith(".cpp") ||
                n.endsWith(".c") ||
                n.endsWith(".h") ||
                n.endsWith(".hpp"))
            return "C ";

        if (n.endsWith(".py"))
            return "🐍 ";

        if (n.endsWith(".js") ||
                n.endsWith(".ts"))
            return "JS ";

        if (n.endsWith(".gradle"))
            return "⚙ ";

        return "📄 ";
    }

    private void deleteRecursive(File f) {

        if (f == null ||
                !f.exists())
            return;

        if (f.isDirectory()) {

            File[] children =
                    f.listFiles();

            if (children != null) {

                for (File child :
                        children) {

                    deleteRecursive(child);
                }
            }
        }

        f.delete();
    }

    private void showInfo(String message) {

        new android.app.AlertDialog.Builder(this)
                .setTitle("DecoderPRO")
                .setMessage(message)
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }

    private void showError(String message) {
        showInfo(message);
    }


    // =========================================================
    // DECODERPRO - BINARY ANALYZER
    // =========================================================

    private void openBinaryAnalyzer(File file) {
        try {
            StringBuilder out = new StringBuilder();

            out.append("DECODERPRO BINARY ANALYZER\n");
            out.append("==============================\n\n");

            out.append("Name      : ").append(file.getName()).append("\n");
            out.append("Path      : ").append(file.getAbsolutePath()).append("\n");
            out.append("Size      : ").append(formatBytes(file.length()))
               .append(" (").append(file.length()).append(" bytes)\n");
            out.append("Extension : ").append(getExtension(file.getName())).append("\n");

            byte[] head = new byte[64];

            RandomAccessFile raf = new RandomAccessFile(file, "r");
            int read = raf.read(head);
            raf.close();

            if (read < 0) read = 0;

            out.append("Type      : ")
               .append(detectBinaryType(head, read, file.getName()))
               .append("\n");

            out.append("\nMAGIC / HEADER\n");
            out.append("==============================\n");
            out.append(hexDump(head, read)).append("\n");

            out.append("SHA-256\n");
            out.append("==============================\n");
            out.append(calculateSha256(file)).append("\n\n");

            double entropy = calculateEntropy(file);

            out.append("ENTROPY\n");
            out.append("==============================\n");
            out.append(String.format(
                    Locale.US,
                    "%.6f / 8.000000\n",
                    entropy
            ));

            if (entropy >= 7.5)
                out.append("Very high entropy\n");
            else if (entropy >= 6.0)
                out.append("High entropy\n");
            else if (entropy >= 4.0)
                out.append("Medium entropy\n");
            else
                out.append("Low entropy\n");

            out.append("\nPRINTABLE STRINGS\n");
            out.append("==============================\n");
            out.append(extractPrintableStrings(file));

            editorText.setText(out.toString());
            editorText.setSelection(0);

            openedLocalFile = null;

            log("Binary analyzer: " + file.getName());

        } catch (Exception e) {
            editorText.setText(
                    "BINARY ANALYZER ERROR\n\n" + e.getMessage()
            );
            log("Binary analyzer error: " + e.getMessage());
        }
    }

    private String formatBytes(long size) {
        if (size < 1024)
            return size + " B";

        if (size < 1024L * 1024L)
            return String.format(Locale.US, "%.2f KB", size / 1024.0);

        if (size < 1024L * 1024L * 1024L)
            return String.format(
                    Locale.US,
                    "%.2f MB",
                    size / (1024.0 * 1024.0)
            );

        return String.format(
                Locale.US,
                "%.2f GB",
                size / (1024.0 * 1024.0 * 1024.0)
        );
    }

    private String getExtension(String name) {
        int p = name.lastIndexOf('.');

        if (p <= 0 || p == name.length() - 1)
            return "(none)";

        return name.substring(p + 1).toLowerCase(Locale.US);
    }

    private String detectBinaryType(
            byte[] b,
            int n,
            String name) {

        if (n >= 4 &&
                (b[0] & 0xff) == 0x7f &&
                b[1] == 'E' &&
                b[2] == 'L' &&
                b[3] == 'F')
            return "ELF executable/shared library";

        if (n >= 4 &&
                b[0] == 'd' &&
                b[1] == 'e' &&
                b[2] == 'x' &&
                b[3] == 0x0a)
            return "DEX";

        if (n >= 4 &&
                b[0] == 'P' &&
                b[1] == 'K' &&
                (b[2] & 0xff) == 0x03 &&
                (b[3] & 0xff) == 0x04)
            return name.toLowerCase(Locale.US).endsWith(".apk")
                    ? "APK / ZIP"
                    : "ZIP archive";

        if (n >= 8 &&
                (b[0] & 0xff) == 0x89 &&
                b[1] == 'P' &&
                b[2] == 'N' &&
                b[3] == 'G')
            return "PNG";

        if (n >= 3 &&
                (b[0] & 0xff) == 0xff &&
                (b[1] & 0xff) == 0xd8 &&
                (b[2] & 0xff) == 0xff)
            return "JPEG";

        if (n >= 4 &&
                b[0] == '%' &&
                b[1] == 'P' &&
                b[2] == 'D' &&
                b[3] == 'F')
            return "PDF";

        if (n >= 4 &&
                b[0] == 'G' &&
                b[1] == 'I' &&
                b[2] == 'F' &&
                b[3] == '8')
            return "GIF";

        String lower = name.toLowerCase(Locale.US);

        if (lower.endsWith(".so"))
            return "Native ELF candidate";

        if (lower.endsWith(".pak"))
            return "Game PAK candidate";

        return "Unknown binary/data";
    }

    private String hexDump(byte[] data, int length) {
        StringBuilder out = new StringBuilder();

        int count = Math.min(length, 64);

        for (int i = 0; i < count; i++) {

            if (i % 16 == 0) {
                out.append(String.format(
                        Locale.US,
                        "%08X  ",
                        i
                ));
            }

            out.append(String.format(
                    Locale.US,
                    "%02X ",
                    data[i] & 0xff
            ));

            if (i % 16 == 15)
                out.append("\n");
        }

        return out.toString();
    }

    private String calculateSha256(File file) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            FileInputStream in = new FileInputStream(file);
            byte[] buffer = new byte[8192];

            int n;

            while ((n = in.read(buffer)) != -1)
                digest.update(buffer, 0, n);

            in.close();

            byte[] hash = digest.digest();

            StringBuilder out = new StringBuilder();

            for (byte b : hash)
                out.append(String.format(
                        Locale.US,
                        "%02x",
                        b & 0xff
                ));

            return out.toString();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private double calculateEntropy(File file) {

        long[] freq = new long[256];
        long total = 0;

        try {
            FileInputStream in = new FileInputStream(file);
            byte[] buffer = new byte[8192];

            int n;

            while ((n = in.read(buffer)) != -1) {

                for (int i = 0; i < n; i++) {
                    freq[buffer[i] & 0xff]++;
                    total++;
                }
            }

            in.close();

        } catch (Exception e) {
            return 0.0;
        }

        if (total == 0)
            return 0.0;

        double entropy = 0.0;

        for (long count : freq) {

            if (count == 0)
                continue;

            double probability =
                    (double) count / (double) total;

            entropy -= probability *
                    (Math.log(probability) / Math.log(2.0));
        }

        return entropy;
    }

    private String extractPrintableStrings(File file) {

        StringBuilder out = new StringBuilder();
        StringBuilder current = new StringBuilder();

        final int minimum = 4;
        final int maximum = 500;

        int found = 0;

        try {
            FileInputStream in = new FileInputStream(file);
            byte[] buffer = new byte[8192];

            int n;

            while ((n = in.read(buffer)) != -1 &&
                    found < maximum) {

                for (int i = 0;
                        i < n && found < maximum;
                        i++) {

                    int c = buffer[i] & 0xff;

                    if (c >= 32 && c <= 126) {

                        current.append((char) c);

                    } else {

                        if (current.length() >= minimum) {
                            out.append(current).append("\n");
                            found++;
                        }

                        current.setLength(0);
                    }
                }
            }

            if (current.length() >= minimum &&
                    found < maximum) {

                out.append(current).append("\n");
            }

            in.close();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage() + "\n";
        }

        if (out.length() == 0)
            return "(no printable strings found)\n";

        return out.toString();
    }

    // =========================================================
    // DECODERPRO - PROJECT WIDE SEARCH
    // =========================================================

    private void searchWholeProject(String query) {

        if (query == null)
            return;

        query = query.trim();

        if (query.length() == 0) {
            editorText.setText(
                    "PROJECT SEARCH\n\nEnter a search term."
            );
            return;
        }

        if (projectRoot == null ||
                !projectRoot.exists()) {

            editorText.setText(
                    "PROJECT SEARCH\n\n" +
                    "Open a folder or ZIP project first."
            );
            return;
        }

        StringBuilder out = new StringBuilder();

        out.append("PROJECT SEARCH\n");
        out.append("==============================\n");
        out.append("Query: ").append(query).append("\n");
        out.append("Root : ")
           .append(projectRoot.getAbsolutePath())
           .append("\n\n");

        int[] files = {0};
        int[] matches = {0};

        searchProjectDirectory(
                projectRoot,
                query.toLowerCase(Locale.US),
                out,
                files,
                matches
        );

        out.append("\n==============================\n");
        out.append("Files scanned : ").append(files[0]).append("\n");
        out.append("Matches       : ").append(matches[0]).append("\n");

        if (matches[0] == 0)
            out.append("\nNo matches found.");

        editorText.setText(out.toString());
        editorText.setSelection(0);

        log(
                "Project search: " +
                query +
                " | files=" +
                files[0] +
                " | matches=" +
                matches[0]
        );
    }

    private void searchProjectDirectory(
            File directory,
            String query,
            StringBuilder out,
            int[] files,
            int[] matches) {

        if (directory == null ||
                !directory.isDirectory())
            return;

        File[] children = directory.listFiles();

        if (children == null)
            return;

        for (File f : children) {

            if (matches[0] >= 1000)
                return;

            if (f.isDirectory()) {

                searchProjectDirectory(
                        f,
                        query,
                        out,
                        files,
                        matches
                );

                continue;
            }

            if (!f.isFile())
                continue;

            if (!isTextName(f.getName()))
                continue;

            if (f.length() > 10L * 1024L * 1024L)
                continue;

            files[0]++;

            try {

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        new FileInputStream(f),
                                        java.nio.charset.StandardCharsets.UTF_8
                                )
                        );

                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {

                    lineNumber++;

                    if (line.toLowerCase(Locale.US)
                            .contains(query)) {

                        matches[0]++;

                        out.append(f.getAbsolutePath())
                           .append(":")
                           .append(lineNumber)
                           .append("\n");

                        out.append("    ")
                           .append(line.trim())
                           .append("\n\n");

                        if (matches[0] >= 1000) {
                            out.append(
                                    "\nSearch stopped at 1000 matches.\n"
                            );
                            break;
                        }
                    }
                }

                reader.close();

            } catch (Exception ignored) {
            }
        }
    }



    private void log(String message) {
        android.util.Log.d("DecoderPRO", message);
    }

}
