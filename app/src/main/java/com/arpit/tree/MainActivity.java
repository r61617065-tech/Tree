package com.arpit.tree;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {
    private static final String PASSWORD = "837286581783825564448793";
    private static final String IMAGE_ZIP = "Images.zip";
    private static final String IMAGE_DIR = "Images";
    private static final int BG = Color.rgb(23, 21, 34);
    private static final int SURFACE = Color.rgb(31, 27, 46);
    private static final int SURFACE_2 = Color.rgb(42, 36, 64);
    private static final int BORDER = Color.rgb(124, 58, 237);
    private static final int TEXT = Color.rgb(245, 243, 255);
    private static final int TEXT_DIM = Color.rgb(184, 179, 201);
    private static final int ACCENT = Color.rgb(168, 85, 247);
    private static final int HIGHLIGHT = Color.rgb(192, 132, 252);
    private static final int RED = Color.rgb(177, 157, 196);

    private FamilyRepository.FamilyData data;
    private FamilyTreeView treeView;
    private EditText searchInput;
    private ImageButton clearButton;
    private LinearLayout resultsBox;
    private File imageDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        );

        try {
            ensureImagesExtracted();
            data = FamilyRepository.load(this);
            setContentView(R.layout.activity_main);
            FrameLayout nativeRoot = findViewById(R.id.nativeRoot);
            nativeRoot.addView(createContent(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ));
            treeView.setData(data);
        } catch (Exception e) {
            TextView error = new TextView(this);
            error.setText("Error loading content");
            error.setTextColor(TEXT);
            error.setGravity(Gravity.CENTER);
            error.setBackgroundColor(BG);
            setContentView(error);
        }
    }

    private View createContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.addView(createHeader());
        root.addView(createSearchArea());

        treeView = new FamilyTreeView(this);
        treeView.setOnMemberClickListener(this::openPanel);
        root.addView(treeView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1
        ));
        return root;
    }

    private View createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(10), dp(16), dp(10));
        header.setBackgroundColor(SURFACE);

        TextView title = new TextView(this);
        title.setText("The Tree - Singh Vansh");
        title.setTextColor(HIGHLIGHT);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.setGravity(Gravity.CENTER_VERTICAL);
        legend.addView(legendItem("M", "Purusha", ACCENT));
        legend.addView(legendItem("F", "Stri", HIGHLIGHT));
        legend.addView(legendItem("*", "App Maker", Color.rgb(216, 180, 254)));
        legend.addView(legendItem("+", "Divangat", RED));
        header.addView(legend);
        return header;
    }

    private View legendItem(String mark, String label, int color) {
        TextView item = new TextView(this);
        item.setText(mark + " " + label);
        item.setTextColor(color);
        item.setTextSize(11);
        item.setPadding(dp(6), 0, 0, 0);
        return item;
    }

    private View createSearchArea() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(10), dp(16), dp(8));
        wrap.setBackgroundColor(BG);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(12), 0, dp(8), 0);
        box.setBackground(rounded(SURFACE_2, BORDER, dp(12), dp(1.5f)));

        TextView icon = new TextView(this);
        icon.setText("Search");
        icon.setTextColor(TEXT_DIM);
        icon.setTextSize(11);
        box.addView(icon);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("Naam ya #ID khojo");
        searchInput.setHintTextColor(TEXT_DIM);
        searchInput.setTextColor(TEXT);
        searchInput.setTextSize(14);
        searchInput.setPadding(dp(8), 0, dp(4), 0);
        searchInput.setBackgroundColor(Color.TRANSPARENT);
        box.addView(searchInput, new LinearLayout.LayoutParams(0, dp(42), 1));

        clearButton = new ImageButton(this);
        clearButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        clearButton.setBackgroundColor(Color.TRANSPARENT);
        clearButton.setColorFilter(TEXT_DIM);
        clearButton.setVisibility(View.GONE);
        box.addView(clearButton, new LinearLayout.LayoutParams(dp(34), dp(34)));
        wrap.addView(box, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        resultsBox = new LinearLayout(this);
        resultsBox.setOrientation(LinearLayout.VERTICAL);
        resultsBox.setVisibility(View.GONE);
        resultsBox.setBackground(rounded(SURFACE_2, BORDER, dp(12), dp(1.5f)));
        LinearLayout.LayoutParams resultsParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        resultsParams.topMargin = dp(6);
        wrap.addView(resultsBox, resultsParams);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                doSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        clearButton.setOnClickListener(v -> {
            searchInput.setText("");
            resultsBox.setVisibility(View.GONE);
            treeView.clearHighlight();
        });
        return wrap;
    }

    private void doSearch(String query) {
        resultsBox.removeAllViews();
        clearButton.setVisibility(query.trim().isEmpty() ? View.GONE : View.VISIBLE);
        if (query.trim().isEmpty()) {
            resultsBox.setVisibility(View.GONE);
            treeView.clearHighlight();
            return;
        }

        String cleaned = query.trim().replaceFirst("^#", "").toLowerCase();
        boolean idSearch = query.trim().startsWith("#") || query.trim().matches("\\d+");
        List<FamilyMember> matches = new ArrayList<>();
        for (FamilyMember member : data.members) {
            boolean matched = idSearch
                ? String.valueOf(member.id).contains(cleaned)
                : member.name.toLowerCase().contains(cleaned)
                    || (member.alias != null && member.alias.toLowerCase().contains(cleaned));
            if (matched) matches.add(member);
            if (matches.size() == 8) break;
        }

        if (matches.isEmpty()) {
            TextView empty = resultText("Koi nahi mila", TEXT_DIM);
            resultsBox.addView(empty);
        } else {
            for (FamilyMember member : matches) {
                TextView row = resultText(member.name + "   #" + member.id, TEXT);
                row.setOnClickListener(v -> {
                    searchInput.setText(member.name);
                    searchInput.setSelection(searchInput.getText().length());
                    resultsBox.setVisibility(View.GONE);
                    treeView.highlightMember(member.id);
                });
                resultsBox.addView(row);
            }
        }
        resultsBox.setVisibility(View.VISIBLE);
    }

    private TextView resultText(String text, int color) {
        TextView row = new TextView(this);
        row.setText(text);
        row.setTextColor(color);
        row.setTextSize(13);
        row.setPadding(dp(14), dp(9), dp(14), dp(9));
        return row;
    }

    private void openPanel(FamilyMember member) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(16));
        panel.setBackground(rounded(SURFACE, BORDER, dp(18), dp(1.5f)));
        scroll.addView(panel);

        FrameLayout header = new FrameLayout(this);
        LinearLayout titleStack = new LinearLayout(this);
        titleStack.setOrientation(LinearLayout.VERTICAL);
        titleStack.setPadding(0, 0, dp(42), dp(8));

        TextView badge = new TextView(this);
        badge.setText("F".equals(member.gender) ? "Stri" : "Purusha");
        badge.setTextColor("F".equals(member.gender) ? HIGHLIGHT : ACCENT);
        badge.setTextSize(11);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        titleStack.addView(badge);

        TextView name = new TextView(this);
        name.setText((member.star ? "* " : "") + member.name);
        name.setTextColor(TEXT);
        name.setTextSize(20);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        titleStack.addView(name);

        if (member.alias != null) {
            TextView alias = new TextView(this);
            alias.setText(member.alias);
            alias.setTextColor(TEXT_DIM);
            alias.setTextSize(13);
            titleStack.addView(alias);
        }
        header.addView(titleStack);

        ImageButton close = new ImageButton(this);
        close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        close.setColorFilter(TEXT_DIM);
        close.setBackground(rounded(SURFACE_2, BORDER, dp(8), dp(1)));
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.TOP | Gravity.END);
        header.addView(close, closeParams);
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(header);

        addRows(panel, member);
        Bitmap photo = loadMemberPhoto(member.name);
        if (photo != null) {
            ImageView image = new ImageView(this);
            image.setImageBitmap(photo);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(SURFACE_2);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(240)
            );
            params.topMargin = dp(10);
            panel.addView(image, params);
        }

        dialog.setContentView(scroll);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(24), dp(360));
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
        dialog.show();
    }

    private void addRows(LinearLayout panel, FamilyMember member) {
        row(panel, "ID", "#" + member.id, false);
        if (!member.wives.isEmpty()) row(panel, "Patni", String.join(", ", member.wives), false);
        if (member.husband != null) row(panel, "Pati", member.husband, false);
        if (member.spouse != null) row(panel, "Sathi", member.spouse, false);
        if (member.parent != null) {
            FamilyMember father = data.byId.get(member.parent);
            if (father != null) {
                String mother = motherName(member, father);
                if (mother != null) row(panel, "Mata", mother, false);
                row(panel, "Pita", father.name, false);
            }
        }
        if (member.birth != null && !member.birth.contains("####")) row(panel, "Janm", member.birth, false);
        if (member.death != null) row(panel, "Nidhan", member.death, true);
        if (member.deceased && member.death == null) row(panel, "Sthiti", "Divangat", true);
        if (member.note != null) row(panel, "Note", member.note, false);
        if (!member.children.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Integer childId : member.children) {
                FamilyMember child = data.byId.get(childId);
                names.add(child == null ? "?" : child.name);
            }
            row(panel, "Santan", member.children.size() + " - " + String.join(", ", names), false);
        }
    }

    private String motherName(FamilyMember member, FamilyMember father) {
        if (father.wives.isEmpty()) return null;
        if (member.mother != null && member.mother > 0 && member.mother <= father.wives.size()) {
            return father.wives.get(member.mother - 1);
        }
        return father.wives.size() == 1 ? father.wives.get(0) : null;
    }

    private void row(LinearLayout panel, String label, String value, boolean warning) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView lab = new TextView(this);
        lab.setText(label);
        lab.setTextColor(TEXT_DIM);
        lab.setTextSize(12);
        row.addView(lab, new LinearLayout.LayoutParams(dp(68), LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(warning ? RED : TEXT);
        val.setTextSize(13);
        val.setLineSpacing(0, 1.15f);
        row.addView(val, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        panel.addView(row);
    }

    private Bitmap loadMemberPhoto(String name) {
        File imageFile = new File(imageDir, name + ".webp");
        if (!imageFile.exists()) {
            return null;
        }
        try {
            byte[] encrypted = readFile(imageFile);
            byte[] decrypted = decrypt(encrypted, PASSWORD);
            return BitmapFactory.decodeByteArray(decrypted, 0, decrypted.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void ensureImagesExtracted() throws Exception {
        imageDir = new File(getFilesDir(), IMAGE_DIR);
        File marker = new File(imageDir, ".ready_" + appVersionCode());
        if (marker.exists()) {
            return;
        }

        deleteRecursively(imageDir);
        if (!imageDir.mkdirs() && !imageDir.isDirectory()) {
            throw new IllegalStateException("Unable to create image directory");
        }

        ZipInputStream zis = new ZipInputStream(getAssets().open(IMAGE_ZIP));
        byte[] buffer = new byte[8192];
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                zis.closeEntry();
                continue;
            }

            String name = new File(entry.getName()).getName();
            if (!name.endsWith(".webp")) {
                zis.closeEntry();
                continue;
            }

            File out = new File(imageDir, name);
            FileOutputStream fos = new FileOutputStream(out);
            int count;
            while ((count = zis.read(buffer)) != -1) {
                fos.write(buffer, 0, count);
            }
            fos.close();
            zis.closeEntry();
        }
        zis.close();

        FileOutputStream markerOut = new FileOutputStream(marker);
        markerOut.write(1);
        markerOut.close();
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    private long appVersionCode() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                return getPackageManager().getPackageInfo(getPackageName(), 0).getLongVersionCode();
            }
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    private byte[] readFile(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = fis.read(buffer)) != -1) {
            bos.write(buffer, 0, count);
        }
        fis.close();
        return bos.toByteArray();
    }

    private static byte[] decrypt(byte[] data, String password) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(password.getBytes("UTF-8"));

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] ivBytes = md5.digest(password.getBytes("UTF-8"));

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(data);
    }

    private GradientDrawable rounded(int fill, int stroke, float radius, float strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke((int) strokeWidth, stroke);
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
