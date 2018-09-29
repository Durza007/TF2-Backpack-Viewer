package com.minder.app.tf2backpack.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.minder.app.tf2backpack.Attribute;
import com.minder.app.tf2backpack.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Patrik on 2018-09-15.
 */

public class GameSchemaOverviewParser {
    private enum Command {
        INSERT,
        OTHER
    }

    private static class SqlCommand {

        public final Command command;
        public final String table;
        public final ContentValues values;

        public SqlCommand(Command command, String table, ContentValues values) {
            this.command = command;
            this.table = table;
            this.values = values;
        }
    }

    private static class StrangeItemLevel {
        public int level;
        public int required_score;
        public String name;
    }

    private SQLiteDatabase sqlDb;

    public GameSchemaOverviewParser(InputStream inputStream, SQLiteDatabase sqlDb) throws IOException {
        this.sqlDb = sqlDb;


        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("result")) {
                parseResult(reader);
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();
        reader.close();

        linkQualities();
    }

    private void parseResult(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("status")) {
                if (reader.nextInt() != 1)
                    throw new IOException("Status not 1");
            } else if (name.equals("attributes")) {
                parseAttributes(reader);
            } else if (name.equals("attribute_controlled_attached_particles")) {
                parseParticleEffects(reader);
            } else if (name.equals("item_levels")) {
                parseStrangeItemLevels(reader);
            } else if (name.equals("kill_eater_score_types")) {
                parseStrangeScoreTypes(reader);
            } else if (name.equals("qualities")) {
                parseQualities(reader);
            } else if (name.equals("qualityNames")) {
                parseQualityNames(reader);
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();
    }


    private void parseAttributes(JsonReader reader) throws IOException {
        Gson gson = new Gson();

        reader.beginArray();
        while (reader.hasNext()) {
            Attribute attribute = gson.fromJson(reader, Attribute.class);
            sqlDb.insert("attributes", null, attribute.getSqlValues());
        }
        reader.endArray();
    }

    private void parseParticleEffects(JsonReader reader) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();

            int id = -1;
            String particleName = null;

            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("id")) {
                    id = reader.nextInt();
                } else if (name.equals("name")) {
                    particleName = reader.nextString();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();

            if (id != -1) {
                final ContentValues values = new ContentValues(2);
                values.put("id", id);
                values.put("name", particleName);

                sqlDb.insert("particles", null, values);
            }
        }
        reader.endArray();
    }

    private void parseStrangeItemLevels(JsonReader reader) throws IOException {
        Gson gson = new Gson();
        reader.beginArray();

        while (reader.hasNext()) {
            reader.beginObject();
            String typeName = null;
            List<StrangeItemLevel> strangeItemLevels = new LinkedList<StrangeItemLevel>();

            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("name")) {
                    typeName = reader.nextString();
                } else if (name.equals("levels")) {
                    reader.beginArray();

                    while (reader.hasNext()) {
                        strangeItemLevels.add(
                                (StrangeItemLevel)gson.fromJson(reader, StrangeItemLevel.class));
                    }

                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }

            reader.endObject();

            if (typeName != null) {
                final ContentValues values = new ContentValues(1);
                values.put("type_name", typeName);
                sqlDb.insert("strange_item_levels", null, values);

                sqlDb.execSQL("CREATE TABLE " + typeName + " (level INTEGER PRIMARY KEY, required_score INTEGER, name TEXT)");

                for (StrangeItemLevel s : strangeItemLevels) {
                    final ContentValues strangeValues = new ContentValues(3);
                    strangeValues.put("level", s.level);
                    strangeValues.put("required_score", s.required_score);
                    strangeValues.put("name", s.name);

                    sqlDb.insert(typeName, null, strangeValues);
                }
            }
        }

        reader.endArray();
    }

    private void parseStrangeScoreTypes(JsonReader reader) throws IOException {
        reader.beginArray();

        while (reader.hasNext()) {
            reader.beginObject();
            int type = -1;
            String typeName = null;
            String levelData = null;

            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("type")) {
                    type = reader.nextInt();
                } else if (name.equals("type_name")) {
                    typeName = reader.nextString();
                } else if (name.equals("level_data")) {
                    levelData = reader.nextString();
                } else {
                    reader.skipValue();
                }
            }

            if (type != -1 && typeName != null && levelData != null) {
                final ContentValues values = new ContentValues(3);
                values.put("type", type);
                values.put("type_name", typeName);
                values.put("level_data", levelData);

                sqlDb.insert("strange_score_types", null, values);
            }

            reader.endObject();
        }

        reader.endArray();
    }

    private Map<String, Integer> qualities;
    private Map<String, String> qualityNames;

    private void parseQualities(JsonReader reader) throws IOException {
        qualities = new HashMap<String, Integer>();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            qualities.put(name, reader.nextInt());
        }
        reader.endObject();
    }

    private void parseQualityNames(JsonReader reader) throws IOException {
        qualityNames = new HashMap<String, String>();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            qualityNames.put(name, reader.nextString());
        }
        reader.endObject();
    }

    private void linkQualities() {
        if (qualities != null && qualityNames != null) {
            Set<String> names = qualities.keySet();

            for (String s : names) {
                final ContentValues values = new ContentValues(2);
                values.put("id", qualities.get(s));
                values.put("name", qualityNames.get(s));

                sqlDb.insert("item_qualities", null, values);
            }
        }
    }

}
