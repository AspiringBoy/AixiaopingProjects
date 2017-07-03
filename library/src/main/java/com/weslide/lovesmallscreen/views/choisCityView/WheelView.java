package com.weslide.lovesmallscreen.views.choisCityView;


        import java.util.LinkedList;
        import java.util.List;
        import android.content.Context;
        import android.database.DataSetObserver;
        import android.graphics.Canvas;
        import android.graphics.Paint;
        import android.graphics.drawable.Drawable;
        import android.graphics.drawable.GradientDrawable;
        import android.graphics.drawable.GradientDrawable.Orientation;
        import android.os.Handler;
        import android.os.Message;
        import android.util.AttributeSet;
        import android.view.GestureDetector;
        import android.view.MotionEvent;
        import android.view.View;
        import android.view.GestureDetector.SimpleOnGestureListener;
        import android.view.ViewGroup.LayoutParams;
        import android.view.animation.Interpolator;
        import android.widget.LinearLayout;
        import android.widget.Scroller;

        import net.aixiaoping.library.R;

/**
 * Created by Dong on 2016/6/14.
 */

public class WheelView extends View {

    //*/
    private int[] SHADOWS_COLORS = new int[] { 0xefE9E9E9,
            0xcfE9E9E9, 0x3fE9E9E9 };

    private static final int ITEM_OFFSET_PERCENT = 0;

    /** Left and right padding value */
    private static final int PADDING = 10;

    /** Default count of visible items */
    private static final int DEF_VISIBLE_ITEMS = 8;

    // Wheel Values
    private int currentItem = 0;

    // Count of visible items
    private int visibleItems = DEF_VISIBLE_ITEMS;

    // Item height
    private int itemHeight = 0;

    // Center Line
    private Drawable centerDrawable;

    // Wheel drawables
    private int wheelBackground = R.drawable.wheel_bg;
    private int wheelForeground = R.drawable.wheel_val;

    // Shadows drawables
    private GradientDrawable topShadow;
    private GradientDrawable bottomShadow;

    // Draw Shadows
    private boolean drawShadows = true;

    // Scrolling
    private WheelScroller scroller;
    private boolean isScrollingPerformed;
    private int scrollingOffset;

    // Cyclic
    boolean isCyclic = false;

    // Items layout
    private LinearLayout itemsLayout;

    // The number of first item in layout
    private int firstItem;

    // View adapter
    private WheelViewAdapter viewAdapter;

    // Recycle
    private WheelRecycle recycle = new WheelRecycle(this);

    // Listeners
    private List<OnWheelChangedListener> changingListeners = new LinkedList<OnWheelChangedListener>();
    private List<OnWheelScrollListener> scrollingListeners = new LinkedList<OnWheelScrollListener>();
    private List<OnWheelClickedListener> clickingListeners = new LinkedList<OnWheelClickedListener>();

    /**
     * Constructor
     */
    public WheelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initData(context);
    }

    /**
     * Constructor
     */
    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initData(context);
    }

    /**
     * Constructor
     */
    public WheelView(Context context) {
        super(context);
        initData(context);
    }

    /**
     * Initializes class data
     * @param context the context
     */
    private void initData(Context context) {
        scroller = new WheelScroller(getContext(), scrollingListener);
    }
    // Scrolling listener
    ScrollingListener scrollingListener = new ScrollingListener() {
        @Override
        public void onStarted() {
            isScrollingPerformed = true;
            notifyScrollingListenersAboutStart();
        }

        @Override
        public void onScroll(int distance) {
            doScroll(distance);

            int height = getHeight();
            if (scrollingOffset > height) {
                scrollingOffset = height;
                scroller.stopScrolling();
            } else if (scrollingOffset < -height) {
                scrollingOffset = -height;
                scroller.stopScrolling();
            }
        }

        @Override
        public void onFinished() {
            if (isScrollingPerformed) {
                notifyScrollingListenersAboutEnd();
                isScrollingPerformed = false;
            }

            scrollingOffset = 0;
            invalidate();
        }

        @Override
        public void onJustify() {
            if (Math.abs(scrollingOffset) > WheelScroller.MIN_DELTA_FOR_SCROLLING) {
                scroller.scroll(scrollingOffset, 0);
            }
        }
    };

    /**
     * Set the the specified scrolling interpolator
     * @param interpolator the interpolator
     */
    public void setInterpolator(Interpolator interpolator) {
        scroller.setInterpolator(interpolator);
    }

    /**
     * Gets count of visible items
     *
     * @return the count of visible items
     */
    public int getVisibleItems() {
        return visibleItems;
    }

    /**
     * Sets the desired count of visible items.
     * Actual amount of visible items depends on wheel layout parameters.
     * To apply changes and rebuild view call measure().
     *
     * @param count the desired count for visible items
     */
    public void setVisibleItems(int count) {
        visibleItems = count;
    }

    /**
     * Gets view adapter
     * @return the view adapter
     */
    public WheelViewAdapter getViewAdapter() {
        return viewAdapter;
    }

    // Adapter listener
    private DataSetObserver dataObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            invalidateWheel(false);
        }

        @Override
        public void onInvalidated() {
            invalidateWheel(true);
        }
    };

    /**
     * Sets view adapter. Usually new adapters contain different views, so
     * it needs to rebuild view by calling measure().
     *
     * @param viewAdapter the view adapter
     */
    public void setViewAdapter(WheelViewAdapter viewAdapter) {
        if (this.viewAdapter != null) {
            this.viewAdapter.unregisterDataSetObserver(dataObserver);
        }
        this.viewAdapter = viewAdapter;
        if (this.viewAdapter != null) {
            this.viewAdapter.registerDataSetObserver(dataObserver);
        }

        invalidateWheel(true);
    }

    /**
     * Adds wheel changing listener
     * @param listener the listener
     */
    public void addChangingListener(OnWheelChangedListener listener) {
        changingListeners.add(listener);
    }

    /**
     * Removes wheel changing listener
     * @param listener the listener
     */
    public void removeChangingListener(OnWheelChangedListener listener) {
        changingListeners.remove(listener);
    }

    /**
     * Notifies changing listeners
     * @param oldValue the old wheel value
     * @param newValue the new wheel value
     */
    protected void notifyChangingListeners(int oldValue, int newValue) {
        for (OnWheelChangedListener listener : changingListeners) {
            listener.onChanged(this, oldValue, newValue);
        }
    }

    /**
     * Adds wheel scrolling listener
     * @param listener the listener
     */
    public void addScrollingListener(OnWheelScrollListener listener) {
        scrollingListeners.add(listener);
    }

    /**
     * Removes wheel scrolling listener
     * @param listener the listener
     */
    public void removeScrollingListener(OnWheelScrollListener listener) {
        scrollingListeners.remove(listener);
    }

    /**
     * Notifies listeners about starting scrolling
     */
    protected void notifyScrollingListenersAboutStart() {
        for (OnWheelScrollListener listener : scrollingListeners) {
            listener.onScrollingStarted(this);
        }
    }

    /**
     * Notifies listeners about ending scrolling
     */
    protected void notifyScrollingListenersAboutEnd() {
        for (OnWheelScrollListener listener : scrollingListeners) {
            listener.onScrollingFinished(this);
        }
    }

    /**
     * Adds wheel clicking listener
     * @param listener the listener
     */
    public void addClickingListener(OnWheelClickedListener listener) {
        clickingListeners.add(listener);
    }

    /**
     * Removes wheel clicking listener
     * @param listener the listener
     */
    public void removeClickingListener(OnWheelClickedListener listener) {
        clickingListeners.remove(listener);
    }

    /**
     * Notifies listeners about clicking
     */
    protected void notifyClickListenersAboutClick(int item) {
        for (OnWheelClickedListener listener : clickingListeners) {
            listener.onItemClicked(this, item);
        }
    }

    /**
     * Gets current value
     *
     * @return the current value
     */
    public int getCurrentItem() {
        return currentItem;
    }

    /**
     * Sets the current item. Does nothing when index is wrong.
     *
     * @param index the item index
     * @param animated the animation flag
     */
    public void setCurrentItem(int index, boolean animated) {
        if (viewAdapter == null || viewAdapter.getItemsCount() == 0) {
            return; // throw?
        }

        int itemCount = viewAdapter.getItemsCount();
        if (index < 0 || index >= itemCount) {
            if (isCyclic) {
                while (index < 0) {
                    index += itemCount;
                }
                index %= itemCount;
            } else{
                return; // throw?
            }
        }
        if (index != currentItem) {
            if (animated) {
                int itemsToScroll = index - currentItem;
                if (isCyclic) {
                    int scroll = itemCount + Math.min(index, currentItem) - Math.max(index, currentItem);
                    if (scroll < Math.abs(itemsToScroll)) {
                        itemsToScroll = itemsToScroll < 0 ? scroll : -scroll;
                    }
                }
                scroll(itemsToScroll, 0);
            } else {
                scrollingOffset = 0;

                int old = currentItem;
                currentItem = index;

                notifyChangingListeners(old, currentItem);

                invalidate();
            }
        }
    }

    /**
     * Sets the current item w/o animation. Does nothing when index is wrong.
     *
     * @param index the item index
     */
    public void setCurrentItem(int index) {
        setCurrentItem(index, false);
    }

    /**
     * Tests if wheel is cyclic. That means before the 1st item there is shown the last one
     * @return true if wheel is cyclic
     */
    public boolean isCyclic() {
        return isCyclic;
    }

    /**
     * Set wheel cyclic flag
     * @param isCyclic the flag to set
     */
    public void setCyclic(boolean isCyclic) {
        this.isCyclic = isCyclic;
        invalidateWheel(false);
    }

    /**
     * Determine whether shadows are drawn
     * @return true is shadows are drawn
     */
    public boolean drawShadows() {
        return drawShadows;
    }

    /**
     * Set whether shadows should be drawn
     * @param drawShadows flag as true or false
     */
    public void setDrawShadows(boolean drawShadows) {
        this.drawShadows = drawShadows;
    }

    /**
     * Set the shadow gradient color
     * @param start
     * @param middle
     * @param end
     */
    public void setShadowColor(int start, int middle, int end) {
        SHADOWS_COLORS = new int[] {start, middle, end};
    }

    /**
     * Sets the drawable for the wheel background
     * @param resource
     */
    public void setWheelBackground(int resource) {
        wheelBackground = resource;
        setBackgroundResource(wheelBackground);
    }

    /**
     * Sets the drawable for the wheel foreground
     * @param resource
     */
    public void setWheelForeground(int resource) {
        wheelForeground = resource;
        centerDrawable = getContext().getResources().getDrawable(wheelForeground);
    }

    /**
     * Invalidates wheel
     * @param clearCaches if true then cached views will be clear
     */
    public void invalidateWheel(boolean clearCaches) {
        if (clearCaches) {
            recycle.clearAll();
            if (itemsLayout != null) {
                itemsLayout.removeAllViews();
            }
            scrollingOffset = 0;
        } else if (itemsLayout != null) {
            // cache all items
            recycle.recycleItems(itemsLayout, firstItem, new ItemsRange());
        }

        invalidate();
    }

    /**
     * Initializes resources
     */
    private void initResourcesIfNecessary() {
        if (centerDrawable == null) {
            centerDrawable = getContext().getResources().getDrawable(wheelForeground);
        }

        if (topShadow == null) {
            topShadow = new GradientDrawable(Orientation.TOP_BOTTOM, SHADOWS_COLORS);
        }

        if (bottomShadow == null) {
            bottomShadow = new GradientDrawable(Orientation.BOTTOM_TOP, SHADOWS_COLORS);
        }

        setBackgroundResource(wheelBackground);
    }

    /**
     * Calculates desired height for layout
     *
     * @param layout
     *            the source layout
     * @return the desired layout height
     */
    private int getDesiredHeight(LinearLayout layout) {
        if (layout != null && layout.getChildAt(0) != null) {
            itemHeight = layout.getChildAt(0).getMeasuredHeight();
        }

        int desired = itemHeight * visibleItems - itemHeight * ITEM_OFFSET_PERCENT / 50;

        return Math.max(desired, getSuggestedMinimumHeight());
    }

    /**
     * Returns height of wheel item
     * @return the item height
     */
    private int getItemHeight() {
        if (itemHeight != 0) {
            return itemHeight;
        }

        if (itemsLayout != null && itemsLayout.getChildAt(0) != null) {
            itemHeight = itemsLayout.getChildAt(0).getHeight();
            return itemHeight;
        }

        return getHeight() / visibleItems;
    }

    /**
     * Calculates control width and creates text layouts
     * @param widthSize the input layout width
     * @param mode the layout mode
     * @return the calculated control width
     */
    private int calculateLayoutWidth(int widthSize, int mode) {
        initResourcesIfNecessary();

        // TODO: make it static
        itemsLayout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        itemsLayout.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int width = itemsLayout.getMeasuredWidth();

        if (mode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            width += 2 * PADDING;

            // Check against our minimum width
            width = Math.max(width, getSuggestedMinimumWidth());

            if (mode == MeasureSpec.AT_MOST && widthSize < width) {
                width = widthSize;
            }
        }

        itemsLayout.measure(MeasureSpec.makeMeasureSpec(width - 2 * PADDING, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

        return width;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        buildViewForMeasuring();

        int width = calculateLayoutWidth(widthSize, widthMode);

        int height;
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            height = getDesiredHeight(itemsLayout);

            if (heightMode == MeasureSpec.AT_MOST) {
                height = Math.min(height, heightSize);
            }
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layout(r - l, b - t);
    }

    /**
     * Sets layouts width and height
     * @param width the layout width
     * @param height the layout height
     */
    private void layout(int width, int height) {
        int itemsWidth = width - 2 * PADDING;

        itemsLayout.layout(0, 0, itemsWidth, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (viewAdapter != null && viewAdapter.getItemsCount() > 0) {
            updateView();

            drawItems(canvas);
            drawCenterRect(canvas);
        }

        if (drawShadows) drawShadows(canvas);
    }

    /**
     * Draws shadows on top and bottom of control
     * @param canvas the canvas for drawing
     */
    private void drawShadows(Canvas canvas) {
		/*/ Modified by wulianghuan 2014-11-25
		int height = (int)(1.5 * getItemHeight());
		//*/
        int height = (int)(3 * getItemHeight());
        //*/
        topShadow.setBounds(0, 0, getWidth(), height);
        topShadow.draw(canvas);

        bottomShadow.setBounds(0, getHeight() - height, getWidth(), getHeight());
        bottomShadow.draw(canvas);
    }

    /**
     * Draws items
     * @param canvas the canvas for drawing
     */
    private void drawItems(Canvas canvas) {
        canvas.save();

        int top = (currentItem - firstItem) * getItemHeight() + (getItemHeight() - getHeight()) / 2;
        canvas.translate(PADDING, - top + scrollingOffset);

        itemsLayout.draw(canvas);

        canvas.restore();
    }

    /**
     * Draws rect for current value
     * @param canvas the canvas for drawing
     */
    private void drawCenterRect(Canvas canvas) {
        int center = getHeight() / 2;
        int offset = (int) (getItemHeight() / 2 * 1.2);
		/*/ Remarked by wulianghuan 2014-11-27  使用自己的画线，而不是描边
		Rect rect = new Rect(left, top, right, bottom)
		centerDrawable.setBounds(bounds)
		centerDrawable.setBounds(0, center - offset, getWidth(), center + offset);
		centerDrawable.draw(canvas);
		//*/
        Paint paint = new Paint();
        paint.setColor(getResources().getColor(R.color.selector_btn_text_1));
        // 设置线宽
        paint.setStrokeWidth((float) 3);
        // 绘制上边直线
        canvas.drawLine(0, center - offset, getWidth(), center - offset, paint);
        // 绘制下边直线
        canvas.drawLine(0, center + offset, getWidth(), center + offset, paint);
        //*/
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || getViewAdapter() == null) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!isScrollingPerformed) {
                    int distance = (int) event.getY() - getHeight() / 2;
                    if (distance > 0) {
                        distance += getItemHeight() / 2;
                    } else {
                        distance -= getItemHeight() / 2;
                    }
                    int items = distance / getItemHeight();
                    if (items != 0 && isValidItemIndex(currentItem + items)) {
                        notifyClickListenersAboutClick(currentItem + items);
                    }
                }
                break;
        }

        return scroller.onTouchEvent(event);
    }

    /**
     * Scrolls the wheel
     * @param delta the scrolling value
     */
    private void doScroll(int delta) {
        scrollingOffset += delta;

        int itemHeight = getItemHeight();
        int count = scrollingOffset / itemHeight;

        int pos = currentItem - count;
        int itemCount = viewAdapter.getItemsCount();

        int fixPos = scrollingOffset % itemHeight;
        if (Math.abs(fixPos) <= itemHeight / 2) {
            fixPos = 0;
        }
        if (isCyclic && itemCount > 0) {
            if (fixPos > 0) {
                pos--;
                count++;
            } else if (fixPos < 0) {
                pos++;
                count--;
            }
            // fix position by rotating
            while (pos < 0) {
                pos += itemCount;
            }
            pos %= itemCount;
        } else {
            //
            if (pos < 0) {
                count = currentItem;
                pos = 0;
            } else if (pos >= itemCount) {
                count = currentItem - itemCount + 1;
                pos = itemCount - 1;
            } else if (pos > 0 && fixPos > 0) {
                pos--;
                count++;
            } else if (pos < itemCount - 1 && fixPos < 0) {
                pos++;
                count--;
            }
        }

        int offset = scrollingOffset;
        if (pos != currentItem) {
            setCurrentItem(pos, false);
        } else {
            invalidate();
        }

        // update offset
        scrollingOffset = offset - count * itemHeight;
        if (scrollingOffset > getHeight()) {
            scrollingOffset = scrollingOffset % getHeight() + getHeight();
        }
    }

    /**
     * Scroll the wheel
     * @param time scrolling duration
     */
    public void scroll(int itemsToScroll, int time) {
        int distance = itemsToScroll * getItemHeight() - scrollingOffset;
        scroller.scroll(distance, time);
    }

    /**
     * Calculates range for wheel items
     * @return the items range
     */
    private ItemsRange getItemsRange() {
        if (getItemHeight() == 0) {
            return null;
        }

        int first = currentItem;
        int count = 1;

        while (count * getItemHeight() < getHeight()) {
            first--;
            count += 2; // top + bottom items
        }

        if (scrollingOffset != 0) {
            if (scrollingOffset > 0) {
                first--;
            }
            count++;

            // process empty items above the first or below the second
            int emptyItems = scrollingOffset / getItemHeight();
            first -= emptyItems;
            count += Math.asin(emptyItems);
        }
        return new ItemsRange(first, count);
    }

    /**
     * Rebuilds wheel items if necessary. Caches all unused items.
     *
     * @return true if items are rebuilt
     */
    private boolean rebuildItems() {
        boolean updated = false;
        ItemsRange range = getItemsRange();
        if (itemsLayout != null) {
            int first = recycle.recycleItems(itemsLayout, firstItem, range);
            updated = firstItem != first;
            firstItem = first;
        } else {
            createItemsLayout();
            updated = true;
        }

        if (!updated) {
            updated = firstItem != range.getFirst() || itemsLayout.getChildCount() != range.getCount();
        }

        if (firstItem > range.getFirst() && firstItem <= range.getLast()) {
            for (int i = firstItem - 1; i >= range.getFirst(); i--) {
                if (!addViewItem(i, true)) {
                    break;
                }
                firstItem = i;
            }
        } else {
            firstItem = range.getFirst();
        }

        int first = firstItem;
        for (int i = itemsLayout.getChildCount(); i < range.getCount(); i++) {
            if (!addViewItem(firstItem + i, false) && itemsLayout.getChildCount() == 0) {
                first++;
            }
        }
        firstItem = first;

        return updated;
    }

    /**
     * Updates view. Rebuilds items and label if necessary, recalculate items sizes.
     */
    private void updateView() {
        if (rebuildItems()) {
            calculateLayoutWidth(getWidth(), MeasureSpec.EXACTLY);
            layout(getWidth(), getHeight());
        }
    }

    /**
     * Creates item layouts if necessary
     */
    private void createItemsLayout() {
        if (itemsLayout == null) {
            itemsLayout = new LinearLayout(getContext());
            itemsLayout.setOrientation(LinearLayout.VERTICAL);
        }
    }

    /**
     * Builds view for measuring
     */
    private void buildViewForMeasuring() {
        // clear all items
        if (itemsLayout != null) {
            recycle.recycleItems(itemsLayout, firstItem, new ItemsRange());
        } else {
            createItemsLayout();
        }

        // add views
        int addItems = visibleItems / 2;
        for (int i = currentItem + addItems; i >= currentItem - addItems; i--) {
            if (addViewItem(i, true)) {
                firstItem = i;
            }
        }
    }

    /**
     * Adds view for item to items layout
     * @param index the item index
     * @param first the flag indicates if view should be first
     * @return true if corresponding item exists and is added
     */
    private boolean addViewItem(int index, boolean first) {
        View view = getItemView(index);
        if (view != null) {
            if (first) {
                itemsLayout.addView(view, 0);
            } else {
                itemsLayout.addView(view);
            }

            return true;
        }

        return false;
    }

    /**
     * Checks whether intem index is valid
     * @param index the item index
     * @return true if item index is not out of bounds or the wheel is cyclic
     */
    private boolean isValidItemIndex(int index) {
        return viewAdapter != null && viewAdapter.getItemsCount() > 0 &&
                (isCyclic || index >= 0 && index < viewAdapter.getItemsCount());
    }

    /**
     * Returns view for specified item
     * @param index the item index
     * @return item view or empty view if index is out of bounds
     */
    private View getItemView(int index) {
        if (viewAdapter == null || viewAdapter.getItemsCount() == 0) {
            return null;
        }
        int count = viewAdapter.getItemsCount();
        if (!isValidItemIndex(index)) {
            return viewAdapter.getEmptyItem(recycle.getEmptyItem(), itemsLayout);
        } else {
            while (index < 0) {
                index = count + index;
            }
        }

        index %= count;
        return viewAdapter.getItem(index, recycle.getItem(), itemsLayout);
    }

    /**
     * Stops scrolling
     */
    public void stopScrolling() {
        scroller.stopScrolling();
    }

    public interface OnWheelChangedListener{
        void onChanged(WheelView wheel, int oldValue, int newValue);
    }

    public interface OnWheelClickedListener{
        void onItemClicked(WheelView wheel, int itemIndex);
    }

    public interface OnWheelScrollListener{
        /**onchange()方法被调用时当前轮位置改变*/
        void onScrollingStarted(WheelView wheel);
        /** 滚动结束时调用的回调方法*/
        void onScrollingFinished(WheelView wheel);
    }


    public class ItemsRange {
        private int first;
        private int count;
        public ItemsRange() {
            this(0, 0);
        }

        public ItemsRange(int first, int count) {
            this.first = first;
            this.count = count;
        }

        public int getFirst() {
            return first;
        }

        public int getLast() {
            return getFirst() + getCount() - 1;
        }
        public int getCount() {
            return count;
        }

        /**测试项是否包含的范围
         */
        public boolean contains(int index) {
            return index >= getFirst() && index <= getLast();
        }
    }

    public interface ScrollingListener {
        void onScroll(int distance);

        void onStarted();

        void onFinished();

        void onJustify();
    }

    /**照片卷轴类处理滚动事件和更新*/
    public class WheelScroller{
        /** Scrolling duration */
        private static final int SCROLLING_DURATION = 400;

        /** Minimum delta for scrolling */
        public static final int MIN_DELTA_FOR_SCROLLING = 1;

        // Listener
        private ScrollingListener listener;

        // Context
        private Context context;

        // Scrolling
        private GestureDetector gestureDetector;
        private Scroller scroller;
        private int lastScrollY;
        private float lastTouchedY;
        private boolean isScrollingPerformed;

        public WheelScroller(Context context, ScrollingListener listener) {
            gestureDetector = new GestureDetector(context, gestureListener);
            gestureDetector.setIsLongpressEnabled(false);

            scroller = new Scroller(context);

            this.listener = listener;
            this.context = context;
        }

        public void setInterpolator(Interpolator interpolator) {
            scroller.forceFinished(true);
            scroller = new Scroller(context, interpolator);
        }

        public void scroll(int distance, int time) {
            scroller.forceFinished(true);

            lastScrollY = 0;

            scroller.startScroll(0, 0, 0, distance, time != 0 ? time : SCROLLING_DURATION);
            setNextMessage(MESSAGE_SCROLL);

            startScrolling();
        }

        /**
         * Stops scrolling
         */
        public void stopScrolling() {
            scroller.forceFinished(true);
        }

        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchedY = event.getY();
                    scroller.forceFinished(true);
                    clearMessages();
                    break;

                case MotionEvent.ACTION_MOVE:
                    // perform scrolling
                    int distanceY = (int)(event.getY() - lastTouchedY);
                    if (distanceY != 0) {
                        startScrolling();
                        listener.onScroll(distanceY);
                        lastTouchedY = event.getY();
                    }
                    break;
            }

            if (!gestureDetector.onTouchEvent(event) && event.getAction() == MotionEvent.ACTION_UP) {
                justify();
            }

            return true;
        }

        // gesture listener
        private SimpleOnGestureListener gestureListener = new SimpleOnGestureListener() {
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // Do scrolling in onTouchEvent() since onScroll() are not call immediately
                //  when user touch and move the wheel
                return true;
            }

            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                lastScrollY = 0;
                final int maxY = 0x7FFFFFFF;
                final int minY = -maxY;
                scroller.fling(0, lastScrollY, 0, (int) -velocityY, 0, 0, minY, maxY);
                setNextMessage(MESSAGE_SCROLL);
                return true;
            }
        };

        // Messages
        private final int MESSAGE_SCROLL = 0;
        private final int MESSAGE_JUSTIFY = 1;

        private void setNextMessage(int message) {
            clearMessages();
            animationHandler.sendEmptyMessage(message);
        }

        private void clearMessages() {
            animationHandler.removeMessages(MESSAGE_SCROLL);
            animationHandler.removeMessages(MESSAGE_JUSTIFY);
        }

        // animation handler
        private Handler animationHandler = new Handler() {
            public void handleMessage(Message msg) {
                scroller.computeScrollOffset();
                int currY = scroller.getCurrY();
                int delta = lastScrollY - currY;
                lastScrollY = currY;
                if (delta != 0) {
                    listener.onScroll(delta);
                }

                // scrolling is not finished when it comes to final Y
                // so, finish it manually
                if (Math.abs(currY - scroller.getFinalY()) < MIN_DELTA_FOR_SCROLLING) {
                    currY = scroller.getFinalY();
                    scroller.forceFinished(true);
                }
                if (!scroller.isFinished()) {
                    animationHandler.sendEmptyMessage(msg.what);
                } else if (msg.what == MESSAGE_SCROLL) {
                    justify();
                } else {
                    finishScrolling();
                }
            }
        };

        private void justify() {
            listener.onJustify();
            setNextMessage(MESSAGE_JUSTIFY);
        }

        private void startScrolling() {
            if (!isScrollingPerformed) {
                isScrollingPerformed = true;
                listener.onStarted();
            }
        }

        void finishScrolling() {
            if (isScrollingPerformed) {
                listener.onFinished();
                isScrollingPerformed = false;
            }
        }
    }

}
