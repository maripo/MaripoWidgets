package org.maripo.android.widget;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;

public class DeepRadioGroup extends LinearLayout {
    private int mCheckedId = -1;
    private CompoundButton.OnCheckedChangeListener mChildOnCheckedChangeListener;
    private boolean mProtectFromCheckedChange = false;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private PassThroughHierarchyChangeListener mPassThroughListener;

    public DeepRadioGroup(Context context) {
        super(context);
        setOrientation(VERTICAL);
        init();
    }

    public DeepRadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mChildOnCheckedChangeListener = new CheckedStateTracker();
        mPassThroughListener = new PassThroughHierarchyChangeListener();
        super.setOnHierarchyChangeListener(mPassThroughListener);
    }

    @Override
    public void setOnHierarchyChangeListener(OnHierarchyChangeListener listener) {
        // the user listener is delegated to our pass-through listener
        mPassThroughListener.mOnHierarchyChangeListener = listener;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (mCheckedId != -1) {
            mProtectFromCheckedChange = true;
            setCheckedStateForView(mCheckedId, true);
            mProtectFromCheckedChange = false;
            setCheckedId(mCheckedId);
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        new TreeScanner(new OnRadioButtonFoundListener() {
            @Override
            public void onRadioButtonFound(RadioButton radioButton) {
                if (radioButton.isChecked()) {
                    mProtectFromCheckedChange = true;
                    if (mCheckedId != -1) {
                        setCheckedStateForView(mCheckedId, false);
                    }
                    mProtectFromCheckedChange = false;
                    setCheckedId(radioButton.getId());
                }
            }
        }).scan(child);

        super.addView(child, index, params);
    }

    public void check(int id) {
        if (id != -1 && (id == mCheckedId)) {
            return;
        }

        if (mCheckedId != -1) {
            setCheckedStateForView(mCheckedId, false);
        }

        if (id != -1) {
            setCheckedStateForView(id, true);
        }

        setCheckedId(id);
    }

    private void setCheckedId(int id) {
        mCheckedId = id;
        if (mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener.onCheckedChanged(this, mCheckedId);
        }
    }

    private void setCheckedStateForView(int viewId, boolean checked) {
        View checkedView = findViewById(viewId);
        if (checkedView != null && checkedView instanceof RadioButton) {
            ((RadioButton) checkedView).setChecked(checked);
        }
    }
    public int getCheckedRadioButtonId() {
        return mCheckedId;
    }
    public void clearCheck() {
        check(-1);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new DeepRadioGroup.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof DeepRadioGroup.LayoutParams;
    }

    @Override
    protected LinearLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    public static class LayoutParams extends LinearLayout.LayoutParams {
        
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(int w, int h, float initWeight) {
            super(w, h, initWeight);
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        @Override
        protected void setBaseAttributes(TypedArray a,
                int widthAttr, int heightAttr) {

            if (a.hasValue(widthAttr)) {
                width = a.getLayoutDimension(widthAttr, "layout_width");
            } else {
                width = WRAP_CONTENT;
            }
            
            if (a.hasValue(heightAttr)) {
                height = a.getLayoutDimension(heightAttr, "layout_height");
            } else {
                height = WRAP_CONTENT;
            }
        }
    }

    public interface OnCheckedChangeListener {
        public void onCheckedChanged(DeepRadioGroup group, int checkedId);
    }

    private class CheckedStateTracker implements CompoundButton.OnCheckedChangeListener {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            // prevents from infinite recursion
            if (mProtectFromCheckedChange) {
                return;
            }

            mProtectFromCheckedChange = true;
            if (mCheckedId != -1) {
                setCheckedStateForView(mCheckedId, false);
            }
            mProtectFromCheckedChange = false;

            int id = buttonView.getId();
            setCheckedId(id);
        }
    }

    private class PassThroughHierarchyChangeListener implements
            ViewGroup.OnHierarchyChangeListener {
        private ViewGroup.OnHierarchyChangeListener mOnHierarchyChangeListener;

        public void onChildViewAdded(final View parent, View child) {
            if (parent == DeepRadioGroup.this) {
                new TreeScanner(new OnRadioButtonFoundListener() {
                    @Override
                    public void onRadioButtonFound(RadioButton radioButton) {
                        int id = radioButton.getId();
                        if (id == View.NO_ID) {
                            id = radioButton.hashCode();
                            radioButton.setId(id);
                        }
                        radioButton.setOnCheckedChangeListener(mChildOnCheckedChangeListener);
    
                    }
                }).scan(child);
            }
            if (mOnHierarchyChangeListener != null) {
                mOnHierarchyChangeListener.onChildViewAdded(parent, child);
            }    
        }

        public void onChildViewRemoved(View parent, View child) {
            if (parent == DeepRadioGroup.this) {
                new TreeScanner (new OnRadioButtonFoundListener() {
                    
                    @Override
                    public void onRadioButtonFound(RadioButton radioButton) {
                        radioButton.setOnCheckedChangeListener(null);
                        
                    }
                }).scan(child);
            }
        }
    }
    

    private class TreeScanner {
        OnRadioButtonFoundListener listener;
        public TreeScanner(OnRadioButtonFoundListener listener) {
            this.listener = listener;
        }
        // Scan Recursively
        public void scan(View child) {
            if (child instanceof RadioButton) {
                listener.onRadioButtonFound((RadioButton)child);
            }
            else if (child instanceof ViewGroup)
            {
                ViewGroup viewGroup = (ViewGroup) child;
                for (int i=0, l=viewGroup.getChildCount(); i<l; i++) {
                    scan(viewGroup.getChildAt(i));
                }
            }
            
        }

    }
    private interface OnRadioButtonFoundListener {
        public void onRadioButtonFound (RadioButton radioButton);
    }
}
