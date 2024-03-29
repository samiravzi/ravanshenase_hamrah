package com.hope.commonsense.Views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by User on 6/12/2017.
 */

public class EllipsizingTextView extends TextView {

    private static final CharSequence ELLIPSIS = "\u2026";
    private static final Pattern DEFAULT_END_PUNCTUATION = Pattern.compile("[\\.!?,;:\u2026]*$", Pattern.DOTALL);
    private final List<EllipsizeListener> mEllipsizeListeners = new ArrayList<>();
    private EllipsizeStrategy mEllipsizeStrategy;
    private boolean isEllipsized;
    private boolean isStale;
    private boolean programmaticChange;
    private CharSequence mFullText;
    private int mMaxLines;
    private float mLineSpacingMult = 1.0f;
    private float mLineAddVertPad = 0.0f;

    private Pattern mEndPunctPattern;

    public EllipsizingTextView(Context context) {
        this(context, null);
    }


    public EllipsizingTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }


    public EllipsizingTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, new int[]{ android.R.attr.maxLines }, defStyle, 0);
        setMaxLines(a.getInt(0, Integer.MAX_VALUE));
        a.recycle();
        setEndPunctuationPattern(DEFAULT_END_PUNCTUATION);
    }

    public void setEndPunctuationPattern(Pattern pattern) {
        mEndPunctPattern = pattern;
    }

    public void addEllipsizeListener(@NonNull EllipsizeListener listener) {
        mEllipsizeListeners.add(listener);
    }

    public void removeEllipsizeListener(EllipsizeListener listener) {
        mEllipsizeListeners.remove(listener);
    }

    public boolean isEllipsized() {
        return isEllipsized;
    }

    @SuppressLint("Override")
    public int getMaxLines() {
        return mMaxLines;
    }

    @Override
    public void setMaxLines(int maxLines) {
        super.setMaxLines(maxLines);
        mMaxLines = maxLines;
        isStale = true;
    }

    public boolean ellipsizingLastFullyVisibleLine() {
        return mMaxLines == Integer.MAX_VALUE;
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        mLineAddVertPad = add;
        mLineSpacingMult = mult;
        super.setLineSpacing(add, mult);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (!programmaticChange) {
            mFullText = text;
            isStale = true;
        }
        super.setText(text, type);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (ellipsizingLastFullyVisibleLine()) isStale = true;
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        if (ellipsizingLastFullyVisibleLine()) isStale = true;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (isStale) resetText();
        super.onDraw(canvas);
    }

    private void resetText() {
        int maxLines = getMaxLines();
        CharSequence workingText = mFullText;
        boolean ellipsized = false;

        if (maxLines != -1) {
            if (mEllipsizeStrategy == null) setEllipsize(null);
            workingText = mEllipsizeStrategy.processText(mFullText);
            ellipsized = !mEllipsizeStrategy.isInLayout(mFullText);
        }

        if (!workingText.equals(getText())) {
            programmaticChange = true;
            try {
                setText(workingText);
            } finally {
                programmaticChange = false;
            }
        }

        isStale = false;
        if (ellipsized != isEllipsized) {
            isEllipsized = ellipsized;
            for (EllipsizeListener listener : mEllipsizeListeners) {
                listener.ellipsizeStateChanged(ellipsized);
            }
        }
    }

    @Override
    public void setEllipsize(TextUtils.TruncateAt where) {
        if (where == null) {
            mEllipsizeStrategy = new EllipsizeNoneStrategy();
            return;
        }

        switch (where) {
            case END:
                mEllipsizeStrategy = new EllipsizeEndStrategy();
                break;
            case START:
                mEllipsizeStrategy = new EllipsizeStartStrategy();
                break;
            case MIDDLE:
                mEllipsizeStrategy = new EllipsizeMiddleStrategy();
                break;
            case MARQUEE:
                super.setEllipsize(where);
                isStale = false;
            default:
                mEllipsizeStrategy = new EllipsizeNoneStrategy();
                break;
        }
    }

    public interface EllipsizeListener {
        void ellipsizeStateChanged(boolean ellipsized);
    }

    private abstract class EllipsizeStrategy {
        public CharSequence processText(CharSequence text) {
            return !isInLayout(text) ? createEllipsizedText(text) : text;
        }

        public boolean isInLayout(CharSequence text) {
            Layout layout = createWorkingLayout(text);
            return layout.getLineCount() <= getLinesCount();
        }

        protected Layout createWorkingLayout(CharSequence workingText) {
            return new StaticLayout(workingText, getPaint(),
                    getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                    Layout.Alignment.ALIGN_NORMAL, mLineSpacingMult,
                    mLineAddVertPad, false /* includepad */);
        }

        protected int getLinesCount() {
            if (ellipsizingLastFullyVisibleLine()) {
                int fullyVisibleLinesCount = getFullyVisibleLinesCount();
                return fullyVisibleLinesCount == -1 ? 1 : fullyVisibleLinesCount;
            } else {
                return mMaxLines;
            }
        }

        protected int getFullyVisibleLinesCount() {
            Layout layout = createWorkingLayout("");
            int height = getHeight() - getCompoundPaddingTop() - getCompoundPaddingBottom();
            int lineHeight = layout.getLineBottom(0);
            return height / lineHeight;
        }

        protected abstract CharSequence createEllipsizedText(CharSequence fullText);
    }

    private class EllipsizeNoneStrategy extends EllipsizeStrategy {
        @Override
        protected CharSequence createEllipsizedText(CharSequence fullText) {
            return fullText;
        }
    }

    private class EllipsizeEndStrategy extends EllipsizeStrategy {
        @Override
        protected CharSequence createEllipsizedText(CharSequence fullText) {
            Layout layout = createWorkingLayout(fullText);
            int cutOffIndex = layout.getLineEnd(mMaxLines - 1);
            int textLength = fullText.length();
            int cutOffLength = textLength - cutOffIndex;
            if (cutOffLength < ELLIPSIS.length()) cutOffLength = ELLIPSIS.length();
            String workingText = TextUtils.substring(fullText, 0, textLength - cutOffLength).trim();
            String strippedText = stripEndPunctuation(workingText);

            while (!isInLayout(strippedText + ELLIPSIS)) {
                int lastSpace = workingText.lastIndexOf(' ');
                if (lastSpace == -1) break;
                workingText = workingText.substring(0, lastSpace).trim();
                strippedText = stripEndPunctuation(workingText);
            }

            workingText = strippedText + ELLIPSIS;
            SpannableStringBuilder dest = new SpannableStringBuilder(workingText);

            if (fullText instanceof Spanned) {
                TextUtils.copySpansFrom((Spanned) fullText, 0, workingText.length(), null, dest, 0);
            }
            return dest;
        }

        public String stripEndPunctuation(CharSequence workingText) {
            return mEndPunctPattern.matcher(workingText).replaceFirst("");
        }
    }

    private class EllipsizeStartStrategy extends EllipsizeStrategy {
        @Override
        protected CharSequence createEllipsizedText(CharSequence fullText) {
            Layout layout = createWorkingLayout(fullText);
            int cutOffIndex = layout.getLineEnd(mMaxLines - 1);
            int textLength = fullText.length();
            int cutOffLength = textLength - cutOffIndex;
            if (cutOffLength < ELLIPSIS.length()) cutOffLength = ELLIPSIS.length();
            String workingText = TextUtils.substring(fullText, cutOffLength, textLength).trim();

            while (!isInLayout(ELLIPSIS + workingText)) {
                int firstSpace = workingText.indexOf(' ');
                if (firstSpace == -1) break;
                workingText = workingText.substring(firstSpace, workingText.length()).trim();
            }

            workingText = ELLIPSIS + workingText;
            SpannableStringBuilder dest = new SpannableStringBuilder(workingText);

            if (fullText instanceof Spanned) {
                TextUtils.copySpansFrom((Spanned) fullText, textLength - workingText.length(),
                        textLength, null, dest, 0);
            }
            return dest;
        }
    }

    private class EllipsizeMiddleStrategy extends EllipsizeStrategy {
        @Override
        protected CharSequence createEllipsizedText(CharSequence fullText) {
            Layout layout = createWorkingLayout(fullText);
            int cutOffIndex = layout.getLineEnd(mMaxLines - 1);
            int textLength = fullText.length();
            int cutOffLength = textLength - cutOffIndex;
            if (cutOffLength < ELLIPSIS.length()) cutOffLength = ELLIPSIS.length();
            cutOffLength += cutOffIndex % 2;    // Make it even.
            String firstPart = TextUtils.substring(
                    fullText, 0, textLength / 2 - cutOffLength / 2).trim();
            String secondPart = TextUtils.substring(
                    fullText, textLength / 2 + cutOffLength / 2, textLength).trim();

            while (!isInLayout(firstPart + ELLIPSIS + secondPart)) {
                int lastSpaceFirstPart = firstPart.lastIndexOf(' ');
                int firstSpaceSecondPart = secondPart.indexOf(' ');
                if (lastSpaceFirstPart == -1 || firstSpaceSecondPart == -1) break;
                firstPart = firstPart.substring(0, lastSpaceFirstPart).trim();
                secondPart = secondPart.substring(firstSpaceSecondPart, secondPart.length()).trim();
            }

            SpannableStringBuilder firstDest = new SpannableStringBuilder(firstPart);
            SpannableStringBuilder secondDest = new SpannableStringBuilder(secondPart);

            if (fullText instanceof Spanned) {
                TextUtils.copySpansFrom((Spanned) fullText, 0, firstPart.length(),
                        null, firstDest, 0);
                TextUtils.copySpansFrom((Spanned) fullText, textLength - secondPart.length(),
                        textLength, null, secondDest, 0);
            }
            return TextUtils.concat(firstDest, ELLIPSIS, secondDest);
        }
    }

}
