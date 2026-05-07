package com.arpit.tree;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FamilyRepository {
    public static class FamilyData {
        public int rootId;
        public final List<FamilyMember> members = new ArrayList<>();
        public final Map<Integer, FamilyMember> byId = new HashMap<>();
    }

    public static FamilyData load(Context context) throws Exception {
        JSONObject json = new JSONObject(readAsset(context, "family_tree.json"));
        FamilyData data = new FamilyData();
        data.rootId = json.getJSONObject("meta").optInt("root_id", 1);

        JSONArray members = json.getJSONArray("members");
        for (int i = 0; i < members.length(); i++) {
            JSONObject item = members.getJSONObject(i);
            FamilyMember member = new FamilyMember();
            member.id = item.getInt("id");
            member.name = item.optString("name", "");
            member.gender = item.optString("gender", "M");
            member.parent = item.has("parent") ? item.optInt("parent") : null;
            member.alias = clean(item.optString("alias", null));
            member.birth = clean(item.optString("birth", null));
            member.death = clean(item.optString("death", null));
            member.deceased = item.optBoolean("deceased", false);
            member.star = item.optBoolean("star", false);
            member.note = clean(item.optString("note", null));
            member.husband = clean(item.optString("husband", null));
            member.spouse = clean(item.optString("spouse", null));
            member.mother = item.has("mother") ? item.optInt("mother") : null;

            Object wife = item.opt("wife");
            if (wife instanceof JSONArray) {
                JSONArray wives = (JSONArray) wife;
                for (int w = 0; w < wives.length(); w++) {
                    String value = clean(wives.optString(w, null));
                    if (value != null) member.wives.add(value);
                }
            } else {
                String value = clean(item.optString("wife", null));
                if (value != null) member.wives.add(value);
            }

            JSONArray children = item.optJSONArray("children");
            if (children != null) {
                for (int c = 0; c < children.length(); c++) {
                    member.children.add(children.getInt(c));
                }
            }

            data.members.add(member);
            data.byId.put(member.id, member);
        }

        return data;
    }

    private static String clean(String value) {
        if (value == null || value.isEmpty() || "?".equals(value)) {
            return null;
        }
        return value;
    }

    private static String readAsset(Context context, String filename) throws Exception {
        InputStream is = context.getAssets().open(filename);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = is.read(buffer)) != -1) {
            bos.write(buffer, 0, count);
        }
        is.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
}
