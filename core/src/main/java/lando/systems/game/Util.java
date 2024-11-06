package lando.systems.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import imgui.ImColor;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class Util {

    private static final String TAG = Util.class.getSimpleName();
    private static final String PREFS_NAME = "sandbox-imgui-prefs";

    public static final int IM_COLOR_WHITE = ImColor.rgba(1f, 1f, 1f, 1f);
    public static final int IM_COLOR_GRAY = ImColor.rgba(0.5f, 0.5f, 0.5f, 1f);

    public static Preferences prefs;
    public static Json json;

    public static void init() {
        Util.prefs = Gdx.app.getPreferences(PREFS_NAME);
        Util.json = new Json();
    }

    public static <T> T getPref(String prefName, Class<T> clazz) {
        Object value;
        if      (ClassReflection.isAssignableFrom(Integer.class, clazz)) value = prefs.getInteger(prefName);
        else if (ClassReflection.isAssignableFrom(Long.class, clazz))    value = prefs.getLong(prefName);
        else if (ClassReflection.isAssignableFrom(Float.class, clazz))   value = prefs.getFloat(prefName);
        else if (ClassReflection.isAssignableFrom(Boolean.class, clazz)) value = prefs.getBoolean(prefName);
        else if (ClassReflection.isAssignableFrom(String.class, clazz))  value = prefs.getString(prefName);
        else {
            try {
                // try to deserialize from json string to clazz instance
                Gdx.app.log(TAG, STR."Unsupported type \{clazz.getSimpleName()} for preference \{prefName}, trying to deserialize from json...");
                return json.fromJson(clazz, prefs.getString(prefName));
            } catch (SerializationException e) {
                throw new GdxRuntimeException(STR."Unsupported type \{clazz.getSimpleName()} for preference \{prefName}, unable to deserialize from json");
            }
        }
        return clazz.cast(value);
    }

    public static <T> void putPref(String prefName, T value) {
        var clazz = value.getClass();
        switch (value) {
            case Integer i -> prefs.putInteger(prefName, i);
            case Long    l -> prefs.putLong(prefName, l);
            case Float   v -> prefs.putFloat(prefName, v);
            case Boolean b -> prefs.putBoolean(prefName, b);
            case String  s -> prefs.putString(prefName, s);
            default -> {
                try {
                    // try to serialize to json string and store
                    Gdx.app.log(TAG, STR."Unsupported type \{clazz.getSimpleName()} for preference \{prefName}, trying to serialize to json...");
                    var str = json.toJson(value, clazz);
                    prefs.putString(prefName, str);
                } catch (SerializationException e) {
                    throw new GdxRuntimeException(STR."Unsupported type \{clazz.getSimpleName()} for preference \{prefName}, unable to serialize to json");
                }
            }
        }
        prefs.flush();
    }

    public static void openUrl(String url) {
        try {
            // Use libGDX's built-in browser opener
            Gdx.net.openURI(url);
        } catch (Exception e) {
            // Fallback method using Java's desktop integration
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                    if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                        desktop.browse(new java.net.URI(url));
                    }
                }
            } catch (Exception ex) {
                Gdx.app.error(TAG, STR."Failed to open URL '\{url}': \{ex.getMessage()}");
            }
        }
    }

    public static File openFileDialog() {
        var fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a file");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        var result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }
}
