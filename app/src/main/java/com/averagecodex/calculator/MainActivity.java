package com.averagecodex.calculator;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.MathContext;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(13, 18, 29);
    private static final int PANEL = Color.rgb(22, 29, 43);
    private static final int DIGIT = Color.rgb(33, 42, 59);
    private static final int OPERATOR = Color.rgb(42, 59, 80);
    private static final int ACCENT = Color.rgb(119, 239, 200);
    private static final int WHITE = Color.rgb(241, 246, 250);
    private static final int MUTED = Color.rgb(156, 171, 191);

    private TextView display;
    private TextView history;
    private String input = "0";
    private BigDecimal stored = null;
    private String pending = null;
    private boolean freshInput = false;
    private boolean error = false;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(14), dp(16), dp(14), dp(14));
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("CALCULATOR");
        title.setTextColor(ACCENT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(13);
        title.setLetterSpacing(0.20f);
        title.setPadding(dp(10), dp(10), dp(10), dp(6));
        root.addView(title);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
        screen.setPadding(dp(20), dp(24), dp(20), dp(22));
        screen.setBackground(roundRect(PANEL, dp(25), -1));
        LinearLayout.LayoutParams screenParams =
            new LinearLayout.LayoutParams(-1, 0, 1.6f);
        screenParams.setMargins(dp(4), dp(10), dp(4), dp(18));
        root.addView(screen, screenParams);

        history = new TextView(this);
        history.setText(" ");
        history.setTextSize(18);
        history.setTextColor(MUTED);
        history.setGravity(Gravity.RIGHT);
        screen.addView(history, new LinearLayout.LayoutParams(-1, -2));

        display = new TextView(this);
        display.setText("0");
        display.setTextSize(48);
        display.setTextColor(WHITE);
        display.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        display.setGravity(Gravity.RIGHT);
        display.setMaxLines(1);
        display.setAutoSizeTextTypeUniformWithConfiguration(20, 50, 2,
            TypedValue.COMPLEX_UNIT_SP);
        LinearLayout.LayoutParams numberParams =
            new LinearLayout.LayoutParams(-1, -2);
        numberParams.topMargin = dp(16);
        screen.addView(display, numberParams);

        String[][] buttons = {
            {"C", "±", "%", "÷"},
            {"7", "8", "9", "×"},
            {"4", "5", "6", "−"},
            {"1", "2", "3", "+"},
            {"⌫", "0", ".", "="}
        };
        LinearLayout keypad = new LinearLayout(this);
        keypad.setOrientation(LinearLayout.VERTICAL);
        root.addView(keypad, new LinearLayout.LayoutParams(-1, 0, 5f));

        for (String[] row : buttons) {
            LinearLayout rowView = new LinearLayout(this);
            rowView.setOrientation(LinearLayout.HORIZONTAL);
            keypad.addView(rowView, new LinearLayout.LayoutParams(-1, 0, 1f));
            for (String key : row) {
                Button button = new Button(this);
                button.setText(key);
                button.setAllCaps(false);
                button.setTextSize(25);
                button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                int color = DIGIT;
                int text = WHITE;
                if (key.equals("=")) {
                    color = ACCENT;
                    text = BG;
                } else if (key.equals("C")) {
                    color = Color.rgb(77, 43, 61);
                    text = Color.rgb(255, 186, 197);
                } else if ("÷×−+".contains(key) || key.equals("%") || key.equals("±")) {
                    color = OPERATOR;
                    text = ACCENT;
                }
                button.setTextColor(text);
                button.setBackground(roundRect(color, dp(18), -1));
                button.setOnClickListener(v -> press(key));
                LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(0, -1, 1f);
                params.setMargins(dp(4), dp(4), dp(4), dp(4));
                rowView.addView(button, params);
            }
        }
    }

    private void press(String key) {
        if (key.equals("C")) {
            clear();
            return;
        }
        if (error) {
            clear();
            if (!"0123456789.".contains(key)) return;
        }
        if (key.length() == 1 && Character.isDigit(key.charAt(0))) {
            if (freshInput || input.equals("0")) {
                input = key;
                freshInput = false;
            } else if (input.replace("-", "").replace(".", "").length() < 15) {
                input += key;
            }
        } else {
            switch (key) {
                case ".":
                    if (freshInput) {
                        input = "0.";
                        freshInput = false;
                    } else if (!input.contains(".")) {
                        input += ".";
                    }
                    break;
                case "±":
                    if (!input.equals("0")) {
                        input = input.startsWith("-") ? input.substring(1) : "-" + input;
                    }
                    break;
                case "%":
                    input = format(new BigDecimal(input)
                        .divide(BigDecimal.valueOf(100), MathContext.DECIMAL64));
                    break;
                case "⌫":
                    if (freshInput) freshInput = false;
                    input = input.length() <= 1 ||
                        (input.length() == 2 && input.startsWith("-"))
                        ? "0" : input.substring(0, input.length() - 1);
                    break;
                case "+":
                case "−":
                case "×":
                case "÷":
                    operator(key);
                    break;
                case "=":
                    equalsPressed();
                    break;
                default:
                    return;
            }
        }
        show();
    }

    private void operator(String op) {
        try {
            if (stored != null && pending != null && !freshInput) {
                stored = calculate(stored, new BigDecimal(input), pending);
                input = format(stored);
            } else {
                stored = new BigDecimal(input);
            }
            pending = op;
            history.setText(input + " " + op);
            freshInput = true;
        } catch (ArithmeticException e) {
            showError();
        }
    }

    private void equalsPressed() {
        if (stored == null || pending == null) return;
        try {
            BigDecimal right = new BigDecimal(input);
            history.setText(format(stored) + " " + pending + " " + input + " =");
            input = format(calculate(stored, right, pending));
            stored = null;
            pending = null;
            freshInput = true;
        } catch (ArithmeticException e) {
            showError();
        }
    }

    private BigDecimal calculate(BigDecimal left, BigDecimal right, String op) {
        switch (op) {
            case "+": return left.add(right);
            case "−": return left.subtract(right);
            case "×": return left.multiply(right);
            case "÷": return left.divide(right, MathContext.DECIMAL64);
            default: return right;
        }
    }

    private String format(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        String plain = stripped.toPlainString();
        if (plain.length() <= 18) return plain;
        return stripped.round(new MathContext(12)).toString();
    }

    private void show() {
        display.setText(error ? "Error" : input);
    }

    private void showError() {
        error = true;
        stored = null;
        pending = null;
        history.setText("Cannot divide by zero");
        show();
    }

    private void clear() {
        stored = null;
        pending = null;
        input = "0";
        freshInput = false;
        error = false;
        history.setText(" ");
        show();
    }

    private GradientDrawable roundRect(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != -1) drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private int dp(float pixels) {
        return (int) (pixels * getResources().getDisplayMetrics().density + 0.5f);
    }
}
