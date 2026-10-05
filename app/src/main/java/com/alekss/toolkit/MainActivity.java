package com.alekss.toolkit;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;

public class MainActivity extends Activity {

    private static final int PICK_FILE = 1001;
    private static final int PICK_FOLDER = 1002;

    // ============================================================
    // COLORS
    // ============================================================

    private static final int BG       = Color.rgb(7, 17, 31);
    private static final int PANEL    = Color.rgb(11, 23, 39);
    private static final int PANEL2   = Color.rgb(14, 29, 49);
    private static final int BORDER   = Color.rgb(24, 50, 74);
    private static final int BLUE     = Color.rgb(36, 168, 255);
    private static final int CYAN     = Color.rgb(82, 217, 255);
    private static final int TEXT     = Color.rgb(216, 233, 247);
    private static final int MUTED    = Color.rgb(120, 147, 170);
    private static final int GREEN    = Color.rgb(73, 209, 125);
    private static final int ORANGE   = Color.rgb(255, 180, 84);
    private static final int RED      = Color.rgb(255, 95, 95);

    // ============================================================
    // UI
    // ============================================================

    private LinearLayout root;
    private LinearLayout explorer;
    private LinearLayout centerArea;
    private LinearLayout rightPanel;
    private LinearLayout bottomPanel;

    private TextView editorTitle;
    private TextView lineNumbers;
    private EditText editorText;

    private TextView statusText;
    private TextView fileInfoText;
    private TextView decompStatsText;

    private File projectRoot;
    private File openedLocalFile;

    private Uri openedSafFile;
    private boolean safEditing = false;

    // ============================================================
    // LIFECYCLE
    // ============================================================

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(Color.rgb(5, 11, 19));
        getWindow().setNavigationBarColor(Color.rgb(5, 11, 19));

        buildIDE();
    }

    // ============================================================
    // MAIN IDE
    // ============================================================

    private void buildIDE() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // --------------------------------------------------------
        // TOP BAR
        // --------------------------------------------------------

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(12), 0, dp(12), 0);
        top.setBackgroundColor(Color.rgb(5, 13, 24));

        TextView logo = new TextView(this);
        logo.setText("◈");
        logo.setTextColor(CYAN);
        logo.setTextSize(25);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        top.addView(
                logo,
                new LinearLayout.LayoutParams(
                        dp(38),
                        dp(52)
                )
        );

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);

        TextView aleks = makeText(
                "ALEKS SOFT",
                11,
                CYAN
        );
        aleks.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView decoder = makeText(
                "DecoderPRO",
                20,
                TEXT
        );
        decoder.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView subtitle = makeText(
                "Reverse Engineering • Decompiler • Crypto • Analyzer",
                8,
                MUTED
        );

        brand.addView(aleks);
        brand.addView(decoder);
        brand.addView(subtitle);

        top.addView(
                brand,
                new LinearLayout.LayoutParams(
                        0,
                        dp(58),
                        1
                )
        );

        Button open = darkButton("OPEN");
        open.setOnClickListener(v -> showOpenMenu());

        top.addView(
                open,
                new LinearLayout.LayoutParams(
                        dp(80),
                        dp(42)
                )
        );

        Button save = darkButton("SAVE");
        save.setOnClickListener(v -> saveCurrentFile());

        top.addView(
                save,
                new LinearLayout.LayoutParams(
                        dp(80),
                        dp(42)
                )
        );

        root.addView(
                top,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(64)
                )
        );

        // --------------------------------------------------------
        // MAIN WORKSPACE
        // --------------------------------------------------------

        LinearLayout workspace = new LinearLayout(this);
        workspace.setOrientation(LinearLayout.HORIZONTAL);
        workspace.setBackgroundColor(BG);

        // --------------------------------------------------------
        // LEFT SIDEBAR
        // --------------------------------------------------------

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setBackgroundColor(PANEL);

        String[] navigation = {
                "EXPLORER",
                "SEARCH",
                "ANALYSIS",
                "CRYPTO",
                "COMPRESS",
                "RE TOOLS",
                "SETTINGS",
                "ABOUT"
        };

        for (String item : navigation) {

            TextView nav = makeText(
                    item,
                    11,
                    item.equals("EXPLORER") ? CYAN : MUTED
            );

            nav.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );

            nav.setPadding(
                    dp(12),
                    dp(11),
                    dp(8),
                    dp(11)
            );

            nav.setOnClickListener(v -> {

                if (item.equals("SEARCH")) {
                    showSearch();
                } else if (item.equals("ANALYSIS")) {
                    showAnalysisInfo();
                } else if (item.equals("CRYPTO")) {
                    showCryptoInfo();
                } else if (item.equals("COMPRESS")) {
                    showCompressionInfo();
                } else if (item.equals("RE TOOLS")) {
                    showRETools();
                } else if (item.equals("SETTINGS")) {
                    showInfo(
                            "SETTINGS\n\n" +
                            "DecoderPRO\n" +
                            "Version 1.1.0\n\n" +
                            "Landscape IDE mode enabled."
                    );
                } else if (item.equals("ABOUT")) {
                    showInfo(
                            "ALEKS SOFT DecoderPRO\n\n" +
                            "Reverse Engineering IDE\n" +
                            "Decompiler • Analyzer • Crypto • RE Tools"
                    );
                }
            });

            left.addView(nav);
        }

        TextView explorerHeader = makeText(
                "PROJECT EXPLORER",
                10,
                MUTED
        );

        explorerHeader.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        explorerHeader.setPadding(
                dp(10),
                dp(14),
                dp(10),
                dp(8)
        );

        left.addView(explorerHeader);

        ScrollView explorerScroll = new ScrollView(this);

        explorer = new LinearLayout(this);
        explorer.setOrientation(LinearLayout.VERTICAL);
        explorer.setPadding(
                dp(5),
                dp(2),
                dp(5),
                dp(10)
        );

        TextView empty = makeText(
                "📁  No project opened\n\nOPEN a ZIP or Folder",
                11,
                MUTED
        );

        empty.setPadding(
                dp(10),
                dp(10),
                dp(10),
                dp(10)
        );

        explorer.addView(empty);

        explorerScroll.addView(explorer);

        left.addView(
                explorerScroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        TextView recent = makeText(
                "RECENT FILES\n\nNo recent files",
                9,
                MUTED
        );

        recent.setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
        );

        left.addView(recent);

        workspace.addView(
                left,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0.18f
                )
        );

        // --------------------------------------------------------
        // CENTER EDITOR
        // --------------------------------------------------------

        centerArea = new LinearLayout(this);
        centerArea.setOrientation(LinearLayout.VERTICAL);
        centerArea.setBackgroundColor(BG);

        LinearLayout editorTabs = new LinearLayout(this);
        editorTabs.setOrientation(LinearLayout.HORIZONTAL);
        editorTabs.setBackgroundColor(PANEL);

        editorTitle = makeText(
                "  NO FILE OPEN",
                11,
                CYAN
        );

        editorTitle.setGravity(Gravity.CENTER_VERTICAL);

        editorTabs.addView(
                editorTitle,
                new LinearLayout.LayoutParams(
                        0,
                        dp(38),
                        1
                )
        );

        TextView close = makeText(
                "×",
                20,
                MUTED
        );

        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> closeEditor());

        editorTabs.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(45),
                        dp(38)
                )
        );

        centerArea.addView(editorTabs);

        // Breadcrumb
        TextView breadcrumb = makeText(
                "DecoderPRO  /  Workspace  /  Editor",
                9,
                MUTED
        );

        breadcrumb.setPadding(
                dp(10),
                dp(5),
                dp(10),
                dp(5)
        );

        breadcrumb.setBackgroundColor(PANEL2);

        centerArea.addView(breadcrumb);

        // Toolbar
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(Color.rgb(9, 20, 34));

        String[] tools = {
                "HEX",
                "STRINGS",
                "HASH",
                "INFO"
        };

        for (String t : tools) {

            Button b = smallButton(t);

            b.setOnClickListener(v -> {

                if (t.equals("HEX")) {
                    showHexForCurrent();
                } else if (t.equals("STRINGS")) {
                    showStringsForCurrent();
                } else if (t.equals("HASH")) {
                    showHashForCurrent();
                } else {
                    showCurrentInfo();
                }
            });

            toolbar.addView(
                    b,
                    new LinearLayout.LayoutParams(
                            dp(75),
                            dp(34)
                    )
            );
        }

        centerArea.addView(toolbar);

        // Code area
        LinearLayout codeArea = new LinearLayout(this);
        codeArea.setOrientation(LinearLayout.HORIZONTAL);
        codeArea.setBackgroundColor(BG);

        lineNumbers = makeText(
                "1",
                11,
                MUTED
        );

        lineNumbers.setGravity(
                Gravity.TOP | Gravity.RIGHT
        );

        lineNumbers.setTypeface(
                Typeface.MONOSPACE
        );

        lineNumbers.setPadding(
                dp(7),
                dp(8),
                dp(9),
                dp(8)
        );

        lineNumbers.setBackgroundColor(
                Color.rgb(6, 14, 25)
        );

        codeArea.addView(
                lineNumbers,
                new LinearLayout.LayoutParams(
                        dp(45),
                        LinearLayout.LayoutParams.MATCH_PARENT
                )
        );

        editorText = new EditText(this);

        editorText.setTextColor(TEXT);
        editorText.setHintTextColor(MUTED);
        editorText.setHint("Open a source, APK, DEX, SO, PAK or other file...");
        editorText.setTextSize(12);
        editorText.setTypeface(Typeface.MONOSPACE);
        editorText.setGravity(Gravity.TOP | Gravity.LEFT);
        editorText.setBackgroundColor(BG);
        editorText.setPadding(
                dp(9),
                dp(8),
                dp(9),
                dp(8)
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
                            android.text.Editable s) {
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

        ScrollView codeScroll = new ScrollView(this);
        codeScroll.setFillViewport(true);
        codeScroll.addView(codeArea);

        centerArea.addView(
                codeScroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        workspace.addView(
                centerArea,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0.52f
                )
        );

        // --------------------------------------------------------
        // RIGHT PANEL
        // --------------------------------------------------------

        rightPanel = new LinearLayout(this);
        rightPanel.setOrientation(LinearLayout.VERTICAL);
        rightPanel.setBackgroundColor(PANEL);

        ScrollView rightScroll = new ScrollView(this);

        LinearLayout rightContent = new LinearLayout(this);
        rightContent.setOrientation(LinearLayout.VERTICAL);
        rightContent.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(12)
        );

        TextView analysisTitle = makeText(
                "FILE ANALYSIS",
                12,
                CYAN
        );

        analysisTitle.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        rightContent.addView(analysisTitle);

        fileInfoText = makeText(
                "Type       : —\n" +
                "Size       : —\n" +
                "Modified   : —\n" +
                "Package    : —",
                10,
                TEXT
        );

        fileInfoText.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        fileInfoText.setBackgroundColor(PANEL2);

        rightContent.addView(fileInfoText);

        LinearLayout actionGrid = new LinearLayout(this);
        actionGrid.setOrientation(LinearLayout.VERTICAL);

        String[][] actions = {
                {"DECOMPILE", "decompile"},
                {"HEX VIEW", "hex"},
                {"STRINGS", "strings"},
                {"INFO", "info"}
        };

        for (String[] action : actions) {

            Button b = darkButton(action[0]);

            b.setOnClickListener(
                    v -> runAction(action[1])
            );

            actionGrid.addView(
                    b,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(38)
                    )
            );
        }

        rightContent.addView(actionGrid);

        addSection(rightContent, "DECOMPILATION");

        decompStatsText = makeText(
                "Classes     : —\n" +
                "Methods     : —\n" +
                "Fields      : —\n" +
                "Status      : Ready",
                10,
                TEXT
        );

        decompStatsText.setPadding(
                dp(8),
                dp(6),
                dp(8),
                dp(6)
        );

        rightContent.addView(decompStatsText);

        addSection(rightContent, "CRYPTO / ENCRYPTION");

        rightContent.addView(
                makeText(
                        "AES • RSA • SM4 • ZUC\n" +
                        "SHA-256 • SHA-1 • MD5\n" +
                        "HMAC • Base64 • Hex\n\n" +
                        "User-provided keys/IVs only.",
                        9,
                        MUTED
                )
        );

        addSection(rightContent, "COMPRESSION");

        rightContent.addView(
                makeText(
                        "zstd • zlib • LZ4",
                        9,
                        TEXT
                )
        );

        addSection(rightContent, "HASH");

        rightContent.addView(
                makeText(
                        "SHA-256 / SHA-1 / MD5\n" +
                        "Entropy / Printable Strings",
                        9,
                        TEXT
                )
        );

        addSection(rightContent, "APK INFORMATION");

        rightContent.addView(
                makeText(
                        "Manifest • DEX • Resources\n" +
                        "Package / Version / SDK",
                        9,
                        MUTED
                )
        );

        addSection(rightContent, "LANGUAGES & CRYPTO LIBS");

        rightContent.addView(
                makeText(
                        "Python\n" +
                        "  pycryptodome • cryptography • gmalg\n" +
                        "  zstandard • zlib\n\n" +
                        "C++\n" +
                        "  OpenSSL • Crypto++ • zstd\n\n" +
                        "C#\n" +
                        "  System.Security.Cryptography • ZstdSharp\n\n" +
                        "Rust\n" +
                        "  aes • cbc • sm4 • zstd\n\n" +
                        "Java\n" +
                        "  BouncyCastle • zstd-jni\n\n" +
                        "Go\n" +
                        "  crypto/aes • crypto/cipher • zstd",
                        8,
                        MUTED
                )
        );

        addSection(rightContent, "RE TOOLS");

        rightContent.addView(
                makeText(
                        "Ghidra / IDA\n" +
                        "rizin / radare2\n" +
                        "x64dbg / WinDbg\n" +
                        "Frida integration point\n" +
                        "UEAESKeyFinder integration point",
                        9,
                        TEXT
                )
        );

        addSection(rightContent, "PAK STRUCTURE");

        rightContent.addView(
                makeText(
                        "Header\n" +
                        "Index — AES-256\n" +
                        "File Data — SM4 / SIMPLE1/2\n" +
                        "Footer — ZUC\n" +
                        "Key — RSA protected",
                        9,
                        MUTED
                )
        );

        rightScroll.addView(rightContent);

        rightPanel.addView(
                rightScroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        workspace.addView(
                rightPanel,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0.30f
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

        // --------------------------------------------------------
        // BOTTOM PANEL
        // --------------------------------------------------------

        bottomPanel = new LinearLayout(this);
        bottomPanel.setOrientation(LinearLayout.HORIZONTAL);
        bottomPanel.setGravity(Gravity.CENTER_VERTICAL);
        bottomPanel.setBackgroundColor(Color.rgb(5, 13, 24));
        bottomPanel.setPadding(
                dp(8),
                dp(4),
                dp(8),
                dp(4)
        );

        String[] bottom = {
                "TERMINAL",
                "BUILD",
                "LOGCAT",
                "PROBLEMS",
                "SEARCH RESULTS"
        };

        for (String bname : bottom) {

            TextView b = makeText(
                    bname,
                    9,
                    MUTED
            );

            b.setGravity(Gravity.CENTER);
            b.setPadding(
                    dp(9),
                    dp(7),
                    dp(9),
                    dp(7)
            );

            b.setOnClickListener(
                    v -> showInfo(
                            bname + "\n\n" +
                            "DecoderPRO workspace panel."
                    )
            );

            bottomPanel.addView(
                    b,
                    new LinearLayout.LayoutParams(
                            0,
                            dp(34),
                            1
                    )
            );
        }

        statusText = makeText(
                "● READY",
                9,
                GREEN
        );

        statusText.setGravity(Gravity.CENTER);

        bottomPanel.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        dp(85),
                        dp(34)
                )
        );

        root.addView(
                bottomPanel,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(42)
                )
        );

        setContentView(root);
    }

    // ============================================================
    // UI HELPERS
    // ============================================================

    private TextView makeText(
            String text,
            float size,
            int color) {

        TextView v = new TextView(this);

        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);

        return v;
    }

    private Button darkButton(String text) {

        Button b = new Button(this);

        b.setText(text);
        b.setTextSize(9);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setBackgroundColor(PANEL2);
        b.setPadding(
                dp(5),
                0,
                dp(5),
                0
        );

        return b;
    }

    private Button smallButton(String text) {

        Button b = darkButton(text);

        b.setTextSize(8);

        return b;
    }

    private void addSection(
            LinearLayout parent,
            String title) {

        TextView t = makeText(
                title,
                10,
                CYAN
        );

        t.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        t.setPadding(
                dp(4),
                dp(14),
                dp(4),
                dp(5)
        );

        parent.addView(t);
    }

    private int dp(int value) {

        return (int) (
                value *
                getResources()
                        .getDisplayMetrics()
                        .density +
                0.5f
        );
    }

    // ============================================================
    // OPEN MENU
    // ============================================================

    private void showOpenMenu() {

        String[] options = {
                "Open File",
                "Import ZIP",
                "Open Folder"
        };

        new android.app.AlertDialog.Builder(this)
                .setTitle("DecoderPRO")
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {
                                pickFile();
                            } else if (which == 1) {
                                pickZip();
                            } else {
                                pickFolder();
                            }
                        }
                )
                .show();
    }

    private void pickFile() {

        Intent i = new Intent(
                Intent.ACTION_OPEN_DOCUMENT
        );

        i.setType("*/*");

        i.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        i.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                i,
                PICK_FILE
        );
    }

    private void pickZip() {

        Intent i = new Intent(
                Intent.ACTION_OPEN_DOCUMENT
        );

        i.setType("application/zip");

        i.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        i.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                i,
                PICK_FILE
        );
    }

    private void pickFolder() {

        Intent i = new Intent(
                Intent.ACTION_OPEN_DOCUMENT_TREE
        );

        i.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                i,
                PICK_FOLDER
        );
    }

    // ============================================================
    // ACTIVITY RESULT
    // ============================================================

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
                data == null) {
            return;
        }

        Uri uri = data.getData();

        if (uri == null) {
            return;
        }

        if (requestCode == PICK_FILE) {

            try {

                getContentResolver()
                        .takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );

            } catch (Exception ignored) {
            }

            String name = getDisplayName(uri);

            if (name == null) {
                name = "selected_file";
            }

            openSafFile(uri, name);

        } else if (requestCode == PICK_FOLDER) {

            openFolder(uri);
        }
    }

    // ============================================================
    // SAF FOLDER
    // ============================================================

    private void openFolder(Uri treeUri) {

        try {

            getContentResolver()
                    .takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );

        } catch (Exception ignored) {
        }

        openedLocalFile = null;
        openedSafFile = null;
        safEditing = true;

        explorer.removeAllViews();

        explorer.addView(
                makeText(
                        "PROJECT EXPLORER",
                        10,
                        CYAN
                )
        );

        explorer.addView(
                makeText(
                        "📁 SELECTED FOLDER",
                        11,
                        GREEN
                )
        );

        addSafChildren(
                treeUri,
                treeUri,
                0
        );

        editorTitle.setText(
                "FOLDER PROJECT"
        );

        editorText.setText(
                "Folder opened.\n\n" +
                "Select a source/config/binary file."
        );

        setStatus("● PROJECT OPEN", GREEN);
    }

    private void addSafChildren(
            Uri treeUri,
            Uri directoryUri,
            int depth) {

        try {

            String documentId;

            if (directoryUri.equals(treeUri)) {

                documentId =
                        DocumentsContract
                                .getTreeDocumentId(treeUri);

            } else {

                documentId =
                        DocumentsContract
                                .getDocumentId(directoryUri);
            }

            Uri childrenUri =
                    DocumentsContract
                            .buildChildDocumentsUriUsingTree(
                                    treeUri,
                                    documentId
                            );

            android.database.Cursor cursor =
                    getContentResolver()
                            .query(
                                    childrenUri,
                                    new String[]{
                                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                            DocumentsContract.Document.COLUMN_MIME_TYPE
                                    },
                                    null,
                                    null,
                                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                            );

            if (cursor == null) {
                return;
            }

            try {

                int idIndex =
                        cursor.getColumnIndex(
                                DocumentsContract.Document.COLUMN_DOCUMENT_ID
                        );

                int nameIndex =
                        cursor.getColumnIndex(
                                DocumentsContract.Document.COLUMN_DISPLAY_NAME
                        );

                int mimeIndex =
                        cursor.getColumnIndex(
                                DocumentsContract.Document.COLUMN_MIME_TYPE
                        );

                while (cursor.moveToNext()) {

                    String id =
                            cursor.getString(idIndex);

                    String name =
                            cursor.getString(nameIndex);

                    String mime =
                            cursor.getString(mimeIndex);

                    Uri child =
                            DocumentsContract
                                    .buildDocumentUriUsingTree(
                                            treeUri,
                                            id
                                    );

                    boolean directory =
                            "vnd.android.document/directory"
                                    .equals(mime);

                    TextView item =
                            makeText(
                                    getPrefix(depth) +
                                    (directory
                                            ? "📁 "
                                            : iconName(name)) +
                                    name,
                                    10,
                                    directory
                                            ? TEXT
                                            : MUTED
                            );

                    item.setPadding(
                            dp(5),
                            dp(5),
                            dp(3),
                            dp(5)
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

                        final Uri fileUri = child;
                        final String fileName = name;

                        item.setOnClickListener(
                                v -> openSafFile(
                                        fileUri,
                                        fileName
                                )
                        );
                    }
                }

            } finally {

                cursor.close();
            }

        } catch (Exception e) {

            logError(
                    "SAF tree error: " +
                    e.getMessage()
            );
        }
    }

    // ============================================================
    // SAF FILE
    // ============================================================

    private void openSafFile(
            Uri uri,
            String name) {

        if (name == null) {
            name = "file";
        }

        if (isDecompilerFile(name)) {

            openSafDecompiledFile(
                    uri,
                    name
            );

            return;
        }

        if (!isTextName(name)) {

            analyzeSafBinary(
                    uri,
                    name
            );

            return;
        }

        try {

            InputStream input =
                    getContentResolver()
                            .openInputStream(uri);

            if (input == null) {
                throw new IOException(
                        "Cannot open file"
                );
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder content =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {

                content.append(line)
                        .append('\n');
            }

            reader.close();

            editorTitle.setText(
                    "  " + name
            );

            editorText.setText(
                    content.toString()
            );

            openedSafFile = uri;
            openedLocalFile = null;
            safEditing = true;

            updateLineNumbers();
            updateFileInfo(
                    name,
                    content.length(),
                    "TEXT"
            );

            setStatus(
                    "● FILE OPEN",
                    GREEN
            );

        } catch (Exception e) {

            showError(
                    "Cannot open file:\n\n" +
                    e.getMessage()
            );
        }
    }

    // ============================================================
    // LOCAL PROJECT
    // ============================================================

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
                        "Cannot create project directory"
                );
            }

            InputStream input =
                    getContentResolver()
                            .openInputStream(uri);

            if (input == null) {
                throw new IOException(
                        "Cannot read ZIP"
                );
            }

            ZipInputStream zip =
                    new ZipInputStream(
                            new BufferedInputStream(input)
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

                    if (parent != null) {
                        parent.mkdirs();
                    }

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

            setStatus(
                    "● ZIP IMPORTED",
                    GREEN
            );

        } catch (Exception e) {

            showError(
                    "ZIP import failed:\n\n" +
                    e.getMessage()
            );
        }
    }

    private void showProject() {

        explorer.removeAllViews();

        explorer.addView(
                makeText(
                        "PROJECT EXPLORER",
                        10,
                        CYAN
                )
        );

        explorer.addView(
                makeText(
                        "📁 " + projectRoot.getName(),
                        11,
                        GREEN
                )
        );

        addLocalDirectory(
                projectRoot,
                0
        );
    }

    private void addLocalDirectory(
            File dir,
            int depth) {

        File[] files =
                dir.listFiles();

        if (files == null) {
            return;
        }

        Arrays.sort(
                files,
                (a, b) -> {

                    if (a.isDirectory() &&
                            !b.isDirectory()) {
                        return -1;
                    }

                    if (!a.isDirectory() &&
                            b.isDirectory()) {
                        return 1;
                    }

                    return a.getName()
                            .compareToIgnoreCase(
                                    b.getName()
                            );
                }
        );

        for (File file : files) {

            TextView item =
                    makeText(
                            getPrefix(depth) +
                            (file.isDirectory()
                                    ? "📁 "
                                    : icon(file)) +
                            file.getName(),
                            10,
                            file.isDirectory()
                                    ? TEXT
                                    : MUTED
                    );

            item.setPadding(
                    dp(5),
                    dp(5),
                    dp(3),
                    dp(5)
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

    // ============================================================
    // LOCAL FILE
    // ============================================================

    private void openLocalFile(File file) {

        if (file == null ||
                !file.exists()) {
            return;
        }

        if (isDecompilerFile(file.getName())) {

            openDecompiledFile(file);
            return;
        }

        if (!isTextName(file.getName())) {

            openBinaryAnalyzer(file);
            return;
        }

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(file),
                                    StandardCharsets.UTF_8
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
                    "  " + file.getName()
            );

            editorText.setText(
                    content.toString()
            );

            openedLocalFile = file;
            openedSafFile = null;
            safEditing = false;

            updateLineNumbers();

            updateFileInfo(
                    file.getName(),
                    file.length(),
                    "TEXT"
            );

            setStatus(
                    "● FILE OPEN",
                    GREEN
            );

        } catch (Exception e) {

            showError(
                    "Cannot open file:\n\n" +
                    e.getMessage()
            );
        }
    }

    // ============================================================
    // JADX DECOMPILER
    // ============================================================

    private boolean isDecompilerFile(String name) {

        if (name == null) {
            return false;
        }

        String n =
                name.toLowerCase(Locale.US);

        return n.endsWith(".class") ||
               n.endsWith(".dex") ||
               n.endsWith(".apk") ||
               n.endsWith(".jar") ||
               n.endsWith(".aar") ||
               n.endsWith(".aab") ||
               n.endsWith(".smali");
    }

    private void openDecompiledFile(File file) {

        editorTitle.setText(
                "  " +
                file.getName() +
                " [DECOMPILING]"
        );

        editorText.setText(
                "DECODERPRO DECOMPILER\n\n" +
                "Loading " +
                file.getName() +
                "...\n\n" +
                "Please wait."
        );

        setStatus(
                "● DECOMPILING",
                ORANGE
        );

        new Thread(() -> {

            try {

                JadxArgs args =
                        new JadxArgs();

                args.setInputFile(file);

                StringBuilder result =
                        new StringBuilder();

                int classes = 0;

                try (JadxDecompiler jadx =
                             new JadxDecompiler(args)) {

                    jadx.load();

                    for (JavaClass cls :
                            jadx.getClasses()) {

                        classes++;

                        result.append(
                                "// ==================================================\n"
                        );

                        result.append(
                                "// CLASS: "
                        );

                        result.append(
                                cls.getFullName()
                        );

                        result.append(
                                "\n// ==================================================\n\n"
                        );

                        result.append(
                                cls.getCode()
                        );

                        result.append(
                                "\n\n"
                        );
                    }
                }

                if (result.length() == 0) {

                    result.append(
                            "// JADX produced no Java source.\n\n"
                    );

                    result.append(
                            "// The file may be unsupported,\n"
                    );

                    result.append(
                            "// obfuscated, or contain no classes.\n"
                    );
                }

                String output =
                        result.toString();

                final int finalClasses =
                        classes;

                runOnUiThread(() -> {

                    editorTitle.setText(
                            "  " +
                            file.getName() +
                            " [DECOMPILED]"
                    );

                    editorText.setText(
                            output
                    );

                    editorText.setSelection(0);

                    openedLocalFile = null;
                    openedSafFile = null;
                    safEditing = false;

                    updateLineNumbers();

                    decompStatsText.setText(
                            "Classes     : " +
                            finalClasses +
                            "\nMethods     : JADX\n" +
                            "Fields      : JADX\n" +
                            "Status      : Decompiled"
                    );

                    updateFileInfo(
                            file.getName(),
                            file.length(),
                            "JADX"
                    );

                    setStatus(
                            "● DECOMPILED",
                            GREEN
                    );
                });

            } catch (Throwable e) {

                final String error =
                        e.getClass()
                                .getSimpleName() +
                        ": " +
                        String.valueOf(
                                e.getMessage()
                        );

                runOnUiThread(() -> {

                    editorTitle.setText(
                            "  " +
                            file.getName() +
                            " [ERROR]"
                    );

                    editorText.setText(
                            "JADX DECOMPILER ERROR\n\n" +
                            error +
                            "\n\n" +
                            "Binary analysis remains available."
                    );

                    updateLineNumbers();

                    setStatus(
                            "● DECOMPILER ERROR",
                            RED
                    );
                });
            }

        }).start();
    }

    private void openSafDecompiledFile(
            Uri uri,
            String name) {

        editorTitle.setText(
                "  " +
                name +
                " [LOADING]"
        );

        editorText.setText(
                "DECODERPRO DECOMPILER\n\n" +
                "Copying file from storage..."
        );

        setStatus(
                "● LOADING",
                ORANGE
        );

        new Thread(() -> {

            File temp = null;

            try {

                String safe =
                        name.replaceAll(
                                "[^a-zA-Z0-9._-]",
                                "_"
                        );

                temp =
                        new File(
                                getCacheDir(),
                                "decode_" +
                                System.currentTimeMillis() +
                                "_" +
                                safe
                        );

                InputStream in =
                        getContentResolver()
                                .openInputStream(uri);

                if (in == null) {
                    throw new IOException(
                            "Cannot open selected file"
                    );
                }

                FileOutputStream out =
                        new FileOutputStream(temp);

                byte[] buffer =
                        new byte[8192];

                int n;

                while ((n =
                        in.read(buffer)) != -1) {

                    out.write(
                            buffer,
                            0,
                            n
                    );
                }

                in.close();
                out.close();

                File finalFile = temp;

                runOnUiThread(
                        () -> openDecompiledFile(
                                finalFile
                        )
                );

            } catch (Throwable e) {

                final String error =
                        e.getClass()
                                .getSimpleName() +
                        ": " +
                        String.valueOf(
                                e.getMessage()
                        );

                runOnUiThread(() -> {

                    editorTitle.setText(
                            "  " +
                            name +
                            " [ERROR]"
                    );

                    editorText.setText(
                            "DECOMPILER ERROR\n\n" +
                            error
                    );

                    setStatus(
                            "● ERROR",
                            RED
                    );
                });
            }

        }).start();
    }

    // ============================================================
    // BINARY ANALYZER
    // ============================================================

    private void openBinaryAnalyzer(File file) {

        try {

            StringBuilder out =
                    new StringBuilder();

            out.append(
                    "ALEKS SOFT DecoderPRO\n"
            );

            out.append(
                    "BINARY ANALYZER\n"
            );

            out.append(
                    "==================================================\n\n"
            );

            out.append(
                    "Name      : "
            );

            out.append(
                    file.getName()
            );

            out.append("\n");

            out.append(
                    "Path      : "
            );

            out.append(
                    file.getAbsolutePath()
            );

            out.append("\n");

            out.append(
                    "Size      : "
            );

            out.append(
                    formatBytes(file.length())
            );

            out.append(
                    " ("
            );

            out.append(
                    file.length()
            );

            out.append(
                    " bytes)\n"
            );

            out.append(
                    "Extension : "
            );

            out.append(
                    getExtension(file.getName())
            );

            out.append("\n");

            byte[] head =
                    new byte[64];

            RandomAccessFile raf =
                    new RandomAccessFile(
                            file,
                            "r"
                    );

            int read =
                    raf.read(head);

            raf.close();

            if (read < 0) {
                read = 0;
            }

            out.append(
                    "Type      : "
            );

            out.append(
                    detectBinaryType(
                            head,
                            read,
                            file.getName()
                    )
            );

            out.append("\n\n");

            out.append(
                    "MAGIC / HEADER\n"
            );

            out.append(
                    "==================================================\n"
            );

            out.append(
                    hexDump(
                            head,
                            read
                    )
            );

            out.append("\n");

            out.append(
                    "SHA-256\n"
            );

            out.append(
                    "==================================================\n"
            );

            out.append(
                    calculateSha256(file)
            );

            out.append("\n\n");

            double entropy =
                    calculateEntropy(file);

            out.append(
                    "ENTROPY\n"
            );

            out.append(
                    "==================================================\n"
            );

            out.append(
                    String.format(
                            Locale.US,
                            "%.6f / 8.000000\n",
                            entropy
                    )
            );

            if (entropy >= 7.5) {

                out.append(
                        "Very high entropy\n"
                );

            } else if (entropy >= 6.0) {

                out.append(
                        "High entropy\n"
                );

            } else if (entropy >= 4.0) {

                out.append(
                        "Medium entropy\n"
                );

            } else {

                out.append(
                        "Low entropy\n"
                );
            }

            out.append("\n");

            out.append(
                    "PRINTABLE STRINGS\n"
            );

            out.append(
                    "==================================================\n"
            );

            out.append(
                    extractPrintableStrings(file)
            );

            editorTitle.setText(
                    "  " +
                    file.getName() +
                    " [BINARY]"
            );

            editorText.setText(
                    out.toString()
            );

            editorText.setSelection(0);

            openedLocalFile = null;
            openedSafFile = null;
            safEditing = false;

            updateLineNumbers();

            updateFileInfo(
                    file.getName(),
                    file.length(),
                    detectBinaryType(
                            head,
                            read,
                            file.getName()
                    )
            );

            setStatus(
                    "● ANALYZED",
                    GREEN
            );

        } catch (Exception e) {

            showError(
                    "Binary analyzer failed:\n\n" +
                    e.getMessage()
            );

            setStatus(
                    "● ERROR",
                    RED
            );
        }
    }

    // ============================================================
    // SAF BINARY
    // ============================================================

    private void analyzeSafBinary(
            Uri uri,
            String name) {

        editorTitle.setText(
                "  " +
                name +
                " [ANALYZING]"
        );

        editorText.setText(
                "DECODERPRO BINARY ANALYZER\n\n" +
                "Reading selected file..."
        );

        setStatus(
                "● ANALYZING",
                ORANGE
        );

        new Thread(() -> {

            File temp = null;

            try {

                String safe =
                        name.replaceAll(
                                "[^a-zA-Z0-9._-]",
                                "_"
                        );

                temp =
                        new File(
                                getCacheDir(),
                                "analyze_" +
                                System.currentTimeMillis() +
                                "_" +
                                safe
                        );

                InputStream in =
                        getContentResolver()
                                .openInputStream(uri);

                if (in == null) {
                    throw new IOException(
                            "Cannot open file"
                    );
                }

                FileOutputStream out =
                        new FileOutputStream(temp);

                byte[] buffer =
                        new byte[8192];

                int n;

                while ((n =
                        in.read(buffer)) != -1) {

                    out.write(
                            buffer,
                            0,
                            n
                    );
                }

                in.close();
                out.close();

                File finalFile = temp;

                runOnUiThread(() ->
                        openBinaryAnalyzer(
                                finalFile
                        )
                );

            } catch (Exception e) {

                runOnUiThread(() -> {

                    editorText.setText(
                            "BINARY ANALYZER ERROR\n\n" +
                            e.getMessage()
                    );

                    setStatus(
                            "● ERROR",
                            RED
                    );
                });
            }

        }).start();
    }

    // ============================================================
    // HEX / STRINGS / HASH
    // ============================================================

    private void showHexForCurrent() {

        if (openedLocalFile == null) {

            showInfo(
                    "HEX VIEW\n\n" +
                    "Open a local binary file first."
            );

            return;
        }

        try {

            RandomAccessFile raf =
                    new RandomAccessFile(
                            openedLocalFile,
                            "r"
                    );

            byte[] buffer =
                    new byte[
                            (int)Math.min(
                                    4096,
                                    openedLocalFile.length()
                            )
                    ];

            int n =
                    raf.read(buffer);

            raf.close();

            editorText.setText(
                    hexDump(
                            buffer,
                            Math.max(n, 0)
                    )
            );

            editorTitle.setText(
                    "  " +
                    openedLocalFile.getName() +
                    " [HEX]"
            );

            updateLineNumbers();

        } catch (Exception e) {

            showError(
                    "HEX view failed:\n\n" +
                    e.getMessage()
            );
        }
    }

    private void showStringsForCurrent() {

        if (openedLocalFile == null) {

            showInfo(
                    "STRINGS\n\n" +
                    "Open a local binary file first."
            );

            return;
        }

        editorText.setText(
                extractPrintableStrings(
                        openedLocalFile
                )
        );

        editorTitle.setText(
                "  " +
                openedLocalFile.getName() +
                " [STRINGS]"
        );

        updateLineNumbers();
    }

    private void showHashForCurrent() {

        if (openedLocalFile == null) {

            showInfo(
                    "HASH\n\n" +
                    "Open a local file first."
            );

            return;
        }

        editorText.setText(
                "HASH ANALYSIS\n\n" +
                "File: " +
                openedLocalFile.getName() +
                "\n\n" +
                "SHA-256:\n" +
                calculateSha256(
                        openedLocalFile
                ) +
                "\n\n" +
                "Entropy:\n" +
                String.format(
                        Locale.US,
                        "%.6f / 8.000000",
                        calculateEntropy(
                                openedLocalFile
                        )
                )
        );

        editorTitle.setText(
                "  HASH ANALYSIS"
        );

        updateLineNumbers();
    }

    // ============================================================
    // SEARCH
    // ============================================================

    private void showSearch() {

        final EditText input =
                new EditText(this);

        input.setHint(
                "Search text..."
        );

        input.setSingleLine(true);
        input.setTextColor(TEXT);

        new android.app.AlertDialog.Builder(this)
                .setTitle(
                        "PROJECT SEARCH"
                )
                .setView(input)
                .setPositiveButton(
                        "SEARCH",
                        (dialog, which) -> {

                            String q =
                                    input.getText()
                                            .toString();

                            searchProject(q);
                        }
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .show();
    }

    private void searchProject(
            String query) {

        if (query == null ||
                query.trim().isEmpty()) {

            return;
        }

        query = query.trim();

        if (projectRoot == null ||
                !projectRoot.exists()) {

            String current =
                    editorText.getText()
                            .toString();

            int position =
                    current.indexOf(query);

            if (position >= 0) {

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

            } else {

                showInfo(
                        "Search\n\n" +
                        "Text not found in current editor."
                );
            }

            return;
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "PROJECT SEARCH\n"
        );

        result.append(
                "==================================================\n"
        );

        result.append(
                "Query: "
        );

        result.append(
                query
        );

        result.append(
                "\n\n"
        );

        int[] files = {0};
        int[] matches = {0};

        searchDirectory(
                projectRoot,
                query.toLowerCase(Locale.US),
                result,
                files,
                matches
        );

        result.append(
                "\n==================================================\n"
        );

        result.append(
                "Files scanned : "
        );

        result.append(
                files[0]
        );

        result.append("\n");

        result.append(
                "Matches       : "
        );

        result.append(
                matches[0]
        );

        editorText.setText(
                result.toString()
        );

        editorText.setSelection(0);

        editorTitle.setText(
                "  SEARCH RESULTS"
        );

        updateLineNumbers();
    }

    private void searchDirectory(
            File directory,
            String query,
            StringBuilder out,
            int[] files,
            int[] matches) {

        if (matches[0] >= 1000) {
            return;
        }

        File[] children =
                directory.listFiles();

        if (children == null) {
            return;
        }

        for (File f : children) {

            if (matches[0] >= 1000) {
                return;
            }

            if (f.isDirectory()) {

                searchDirectory(
                        f,
                        query,
                        out,
                        files,
                        matches
                );

                continue;
            }

            if (!f.isFile()) {
                continue;
            }

            if (!isTextName(f.getName())) {
                continue;
            }

            if (f.length() >
                    10L * 1024L * 1024L) {

                continue;
            }

            files[0]++;

            try {

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        new FileInputStream(f),
                                        StandardCharsets.UTF_8
                                )
                        );

                String line;
                int lineNo = 0;

                while ((line =
                        reader.readLine()) != null) {

                    lineNo++;

                    if (line.toLowerCase(
                            Locale.US
                    ).contains(query)) {

                        matches[0]++;

                        out.append(
                                f.getAbsolutePath()
                        );

                        out.append(":");

                        out.append(
                                lineNo
                        );

                        out.append("\n    ");

                        out.append(
                                line.trim()
                        );

                        out.append(
                                "\n\n"
                        );

                        if (matches[0] >= 1000) {
                            break;
                        }
                    }
                }

                reader.close();

            } catch (Exception ignored) {
            }
        }
    }

    // ============================================================
    // SAVE
    // ============================================================

    private void saveCurrentFile() {

        String content =
                editorText.getText()
                        .toString();

        try {

            if (openedLocalFile != null) {

                FileOutputStream out =
                        new FileOutputStream(
                                openedLocalFile
                        );

                out.write(
                        content.getBytes(
                                StandardCharsets.UTF_8
                        )
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

                if (out == null) {

                    throw new IOException(
                            "Cannot write file"
                    );
                }

                out.write(
                        content.getBytes(
                                StandardCharsets.UTF_8
                        )
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
                    "No editable file open",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            showError(
                    "Save failed:\n\n" +
                    e.getMessage()
            );
        }
    }

    // ============================================================
    // ACTIONS
    // ============================================================

    private void runAction(String action) {

        if (action.equals("decompile")) {

            if (openedLocalFile != null) {

                openDecompiledFile(
                        openedLocalFile
                );

            } else if (openedSafFile != null) {

                openSafDecompiledFile(
                        openedSafFile,
                        getDisplayName(
                                openedSafFile
                        )
                );

            } else {

                showInfo(
                        "DECOMPILE\n\n" +
                        "Open APK / DEX / CLASS / JAR first."
                );
            }

            return;
        }

        if (action.equals("hex")) {
            showHexForCurrent();
            return;
        }

        if (action.equals("strings")) {
            showStringsForCurrent();
            return;
        }

        showCurrentInfo();
    }

    private void showCurrentInfo() {

        if (openedLocalFile == null) {

            showInfo(
                    "FILE INFO\n\n" +
                    "No local file is currently selected."
            );

            return;
        }

        updateFileInfo(
                openedLocalFile.getName(),
                openedLocalFile.length(),
                "Local file"
        );

        showInfo(
                "FILE INFORMATION\n\n" +
                "Name: " +
                openedLocalFile.getName() +
                "\n\nPath:\n" +
                openedLocalFile.getAbsolutePath() +
                "\n\nSize: " +
                formatBytes(
                        openedLocalFile.length()
                ) +
                "\n\nSHA-256:\n" +
                calculateSha256(
                        openedLocalFile
                )
        );
    }

    // ============================================================
    // INFO PANELS
    // ============================================================

    private void showAnalysisInfo() {

        showInfo(
                "ANALYSIS\n\n" +
                "Binary analyzer\n" +
                "HEX viewer\n" +
                "Printable strings\n" +
                "SHA-256\n" +
                "Entropy\n" +
                "File type detection\n" +
                "APK / DEX / ELF / PAK candidates"
        );
    }

    private void showCryptoInfo() {

        showInfo(
                "CRYPTO\n\n" +
                "AES\n" +
                "RSA\n" +
                "SM4\n" +
                "ZUC\n" +
                "SHA-256 / SHA-1 / MD5\n" +
                "HMAC\n" +
                "Base64 / Hex\n\n" +
                "Crypto operations should use user-provided keys/IVs."
        );
    }

    private void showCompressionInfo() {

        showInfo(
                "COMPRESSION\n\n" +
                "zstd\n" +
                "zlib\n" +
                "LZ4\n\n" +
                "Compression analysis and decompression integration."
        );
    }

    private void showRETools() {

        showInfo(
                "RE TOOLS\n\n" +
                "Ghidra / IDA — native analysis\n" +
                "rizin / radare2 — Termux\n" +
                "x64dbg / WinDbg — Windows\n" +
                "Frida — runtime integration\n" +
                "UEAESKeyFinder — integration point\n\n" +
                "Use only on software/files you are authorized to analyze."
        );
    }

    // ============================================================
    // FILE INFO
    // ============================================================

    private void updateFileInfo(
            String name,
            long size,
            String type) {

        if (fileInfoText == null) {
            return;
        }

        fileInfoText.setText(
                "Type       : " +
                type +
                "\n" +
                "Name       : " +
                name +
                "\n" +
                "Size       : " +
                formatBytes(size) +
                "\n" +
                "Modified   : —\n" +
                "Package    : —"
        );
    }

    // ============================================================
    // LINE NUMBERS
    // ============================================================

    private void updateLineNumbers() {

        if (editorText == null ||
                lineNumbers == null) {

            return;
        }

        String text =
                editorText.getText()
                        .toString();

        int lines = 1;

        for (int i = 0;
             i < text.length();
             i++) {

            if (text.charAt(i) == '\n') {
                lines++;
            }
        }

        StringBuilder nums =
                new StringBuilder();

        for (int i = 1;
             i <= lines;
             i++) {

            nums.append(i)
                    .append('\n');
        }

        lineNumbers.setText(
                nums.toString()
        );
    }

    // ============================================================
    // CLOSE
    // ============================================================

    private void closeEditor() {

        editorTitle.setText(
                "  NO FILE OPEN"
        );

        editorText.setText("");

        openedLocalFile = null;
        openedSafFile = null;
        safEditing = false;

        updateLineNumbers();

        setStatus(
                "● READY",
                GREEN
        );
    }

    // ============================================================
    // BINARY HELPERS
    // ============================================================

    private String formatBytes(long size) {

        if (size < 1024) {
            return size + " B";
        }

        if (size < 1024L * 1024L) {

            return String.format(
                    Locale.US,
                    "%.2f KB",
                    size / 1024.0
            );
        }

        if (size < 1024L *
                1024L *
                1024L) {

            return String.format(
                    Locale.US,
                    "%.2f MB",
                    size /
                            (1024.0 * 1024.0)
            );
        }

        return String.format(
                Locale.US,
                "%.2f GB",
                size /
                        (1024.0 *
                         1024.0 *
                         1024.0)
        );
    }

    private String getExtension(
            String name) {

        if (name == null) {
            return "(none)";
        }

        int p =
                name.lastIndexOf('.');

        if (p <= 0 ||
                p == name.length() - 1) {

            return "(none)";
        }

        return name.substring(
                p + 1
        ).toLowerCase(
                Locale.US
        );
    }

    private String detectBinaryType(
            byte[] b,
            int n,
            String name) {

        if (n >= 4 &&
                (b[0] & 0xff) == 0x7f &&
                b[1] == 'E' &&
                b[2] == 'L' &&
                b[3] == 'F') {

            return "ELF executable/shared library";
        }

        if (n >= 4 &&
                b[0] == 'd' &&
                b[1] == 'e' &&
                b[2] == 'x' &&
                b[3] == 0x0a) {

            return "DEX";
        }

        if (n >= 4 &&
                b[0] == 'P' &&
                b[1] == 'K' &&
                (b[2] & 0xff) == 0x03 &&
                (b[3] & 0xff) == 0x04) {

            String lower =
                    name == null
                            ? ""
                            : name.toLowerCase(
                                    Locale.US
                            );

            if (lower.endsWith(".apk")) {
                return "APK / ZIP";
            }

            if (lower.endsWith(".jar")) {
                return "JAR / ZIP";
            }

            if (lower.endsWith(".aar")) {
                return "AAR / ZIP";
            }

            if (lower.endsWith(".aab")) {
                return "AAB / ZIP";
            }

            return "ZIP archive";
        }

        if (n >= 4 &&
                b[0] == '%' &&
                b[1] == 'P' &&
                b[2] == 'D' &&
                b[3] == 'F') {

            return "PDF";
        }

        String lower =
                name == null
                        ? ""
                        : name.toLowerCase(
                                Locale.US
                        );

        if (lower.endsWith(".so")) {
            return "Native ELF candidate";
        }

        if (lower.endsWith(".pak")) {
            return "Game PAK candidate";
        }

        if (lower.endsWith(".class")) {
            return "Java CLASS";
        }

        return "Unknown binary/data";
    }

    private String hexDump(
            byte[] data,
            int length) {

        StringBuilder out =
                new StringBuilder();

        int count =
                Math.min(
                        length,
                        4096
                );

        for (int i = 0;
             i < count;
             i++) {

            if (i % 16 == 0) {

                out.append(
                        String.format(
                                Locale.US,
                                "%08X  ",
                                i
                        )
                );
            }

            out.append(
                    String.format(
                            Locale.US,
                            "%02X ",
                            data[i] & 0xff
                    )
            );

            if (i % 16 == 15) {
                out.append('\n');
            }
        }

        return out.toString();
    }

    private String calculateSha256(
            File file) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            FileInputStream in =
                    new FileInputStream(file);

            byte[] buffer =
                    new byte[8192];

            int n;

            while ((n =
                    in.read(buffer)) != -1) {

                digest.update(
                        buffer,
                        0,
                        n
                );
            }

            in.close();

            byte[] hash =
                    digest.digest();

            StringBuilder out =
                    new StringBuilder();

            for (byte b : hash) {

                out.append(
                        String.format(
                                Locale.US,
                                "%02x",
                                b & 0xff
                        )
                );
            }

            return out.toString();

        } catch (Exception e) {

            return "ERROR: " +
                    e.getMessage();
        }
    }

    private double calculateEntropy(
            File file) {

        long[] freq =
                new long[256];

        long total = 0;

        try {

            FileInputStream in =
                    new FileInputStream(file);

            byte[] buffer =
                    new byte[8192];

            int n;

            while ((n =
                    in.read(buffer)) != -1) {

                for (int i = 0;
                     i < n;
                     i++) {

                    freq[
                            buffer[i] &
                            0xff
                    ]++;

                    total++;
                }
            }

            in.close();

        } catch (Exception e) {

            return 0.0;
        }

        if (total == 0) {
            return 0.0;
        }

        double entropy = 0.0;

        for (long count : freq) {

            if (count == 0) {
                continue;
            }

            double p =
                    (double) count /
                    (double) total;

            entropy -=
                    p *
                    (Math.log(p) /
                     Math.log(2.0));
        }

        return entropy;
    }

    private String extractPrintableStrings(
            File file) {

        StringBuilder out =
                new StringBuilder();

        StringBuilder current =
                new StringBuilder();

        final int minimum = 4;
        final int maximum = 500;

        int found = 0;

        try {

            FileInputStream in =
                    new FileInputStream(file);

            byte[] buffer =
                    new byte[8192];

            int n;

            while ((n =
                    in.read(buffer)) != -1 &&
                    found < maximum) {

                for (int i = 0;
                     i < n &&
                     found < maximum;
                     i++) {

                    int c =
                            buffer[i] & 0xff;

                    if (c >= 32 &&
                            c <= 126) {

                        current.append(
                                (char)c
                        );

                    } else {

                        if (current.length()
                                >= minimum) {

                            out.append(
                                    current
                            );

                            out.append('\n');

                            found++;
                        }

                        current.setLength(0);
                    }
                }
            }

            if (current.length()
                    >= minimum &&
                    found < maximum) {

                out.append(current)
                        .append('\n');
            }

            in.close();

        } catch (Exception e) {

            return "ERROR: " +
                    e.getMessage() +
                    "\n";
        }

        if (out.length() == 0) {

            return "(no printable strings found)\n";
        }

        return out.toString();
    }

    // ============================================================
    // TEXT / FILE HELPERS
    // ============================================================

    private boolean isTextName(
            String name) {

        if (name == null) {
            return false;
        }

        String n =
                name.toLowerCase(
                        Locale.US
                );

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

            if (n.endsWith(e)) {
                return true;
            }
        }

        return false;
    }

    private String getPrefix(
            int depth) {

        StringBuilder s =
                new StringBuilder();

        for (int i = 0;
             i < depth;
             i++) {

            s.append("    ");
        }

        return s.toString();
    }

    private String icon(
            File file) {

        return iconName(
                file.getName()
        );
    }

    private String iconName(
            String name) {

        if (name == null) {
            return "📄 ";
        }

        String n =
                name.toLowerCase(
                        Locale.US
                );

        if (n.endsWith(".java") ||
                n.endsWith(".kt")) {

            return "☕ ";
        }

        if (n.endsWith(".xml")) {
            return "🧩 ";
        }

        if (n.endsWith(".json")) {
            return "{} ";
        }

        if (n.endsWith(".cpp") ||
                n.endsWith(".c") ||
                n.endsWith(".h") ||
                n.endsWith(".hpp")) {

            return "C ";
        }

        if (n.endsWith(".py")) {
            return "PY ";
        }

        if (n.endsWith(".js") ||
                n.endsWith(".ts")) {

            return "JS ";
        }

        if (n.endsWith(".apk")) {
            return "APK ";
        }

        if (n.endsWith(".dex")) {
            return "DEX ";
        }

        if (n.endsWith(".so")) {
            return "SO ";
        }

        if (n.endsWith(".pak")) {
            return "PAK ";
        }

        if (n.endsWith(".class")) {
            return "CLS ";
        }

        if (n.endsWith(".gradle")) {
            return "⚙ ";
        }

        return "📄 ";
    }

    // ============================================================
    // SAF NAME
    // ============================================================

    private String getDisplayName(
            Uri uri) {

        try {

            android.database.Cursor cursor =
                    getContentResolver()
                            .query(
                                    uri,
                                    new String[]{
                                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                                    },
                                    null,
                                    null,
                                    null
                            );

            if (cursor == null) {
                return null;
            }

            try {

                if (cursor.moveToFirst()) {

                    int index =
                            cursor.getColumnIndex(
                                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                            );

                    if (index >= 0) {
                        return cursor.getString(index);
                    }
                }

            } finally {

                cursor.close();
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    // ============================================================
    // MISC
    // ============================================================

    private void deleteRecursive(
            File f) {

        if (f == null ||
                !f.exists()) {

            return;
        }

        if (f.isDirectory()) {

            File[] children =
                    f.listFiles();

            if (children != null) {

                for (File child :
                        children) {

                    deleteRecursive(
                            child
                    );
                }
            }
        }

        f.delete();
    }

    private void setStatus(
            String text,
            int color) {

        if (statusText != null) {

            statusText.setText(text);
            statusText.setTextColor(color);
        }
    }

    private void showInfo(
            String message) {

        new android.app.AlertDialog.Builder(this)
                .setTitle(
                        "Aleks Soft DecoderPRO"
                )
                .setMessage(message)
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }

    private void showError(
            String message) {

        showInfo(message);
    }

    private void logError(
            String message) {

        android.util.Log.e(
                "DecoderPRO",
                message
        );
    }
}
