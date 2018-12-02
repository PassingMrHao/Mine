package com.coderpage.mine.app.tally.module.chart;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.util.Pair;

import com.coderpage.base.common.Callback;
import com.coderpage.base.common.IError;
import com.coderpage.base.common.SimpleCallback;
import com.coderpage.base.utils.LogUtils;
import com.coderpage.framework.BaseViewModel;
import com.coderpage.mine.app.tally.common.utils.TallyUtils;
import com.coderpage.mine.app.tally.module.chart.data.CategoryData;
import com.coderpage.mine.app.tally.module.chart.data.DailyData;
import com.coderpage.mine.app.tally.module.chart.data.Month;
import com.coderpage.mine.app.tally.module.chart.data.MonthlyData;
import com.coderpage.mine.app.tally.module.chart.data.MonthlyDataList;
import com.coderpage.mine.app.tally.ui.widget.MonthSelectDialog;
import com.coderpage.mine.app.tally.utils.DateUtils;
import com.coderpage.mine.app.tally.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lc. 2018-09-24 15:34
 * @since 0.6.0
 *
 * 展示 年账单折线图、日支出柱状图、日收入柱状图、支出分类饼图、收入分类饼图
 */

public class TallyChartViewModel extends BaseViewModel {

    private static final String TAG = LogUtils.makeLogTag(TallyChartViewModel.class);

    /** 账单开始日期 */
    private Calendar mStartDate;
    /** 账单结束日期 */
    private Calendar mEndDate;

    private List<Month> mSelectableMonthList = new ArrayList<>();

    /** 当前时间 */
    private ObservableField<String> mCurrentDateText = new ObservableField<>("");
    /** 支出总额 */
    private ObservableField<String> mExpenseTotalAmountText = new ObservableField<>("");
    /** 收入总额 */
    private ObservableField<String> mIncomeTotalAmountText = new ObservableField<>("");
    /** 是否显示为日账单 */
    private ObservableBoolean mDisplayDailyChart = new ObservableBoolean(true);
    /** 是否显示为支出账单 */
    private ObservableBoolean mDisplayExpenseChart = new ObservableBoolean(true);

    private List<DailyData> mDailyExpenseList = new ArrayList<>();
    private List<DailyData> mDailyIncomeList = new ArrayList<>();
    private List<MonthlyData> mMonthlyExpenseList = new ArrayList<>();
    private List<MonthlyData> mMonthlyIncomeList = new ArrayList<>();
    private List<CategoryData> mCategoryDailyExpenseList = new ArrayList<>();
    private List<CategoryData> mCategoryDailyIncomeList = new ArrayList<>();
    private List<CategoryData> mCategoryYearlyExpenseList = new ArrayList<>();
    private List<CategoryData> mCategoryYearlyIncomeList = new ArrayList<>();

    /** 月单位的收入列表 */
    private MutableLiveData<MonthlyDataList> mObservableMonthlyDataList = new MutableLiveData<>();
    /** 日单位的支出列表 */
    private MutableLiveData<List<DailyData>> mObservableDailyExpenseList = new MutableLiveData<>();
    /** 日单位的收入列表 */
    private MutableLiveData<List<DailyData>> mObservableDailyIncomeList = new MutableLiveData<>();
    /** 消费分类金额列表 */
    private MutableLiveData<List<CategoryData>> mObservableCategoryExpenseDataList = new MutableLiveData<>();
    /** 收入分类金额列表 */
    private MutableLiveData<List<CategoryData>> mObservableCategoryIncomeDataList = new MutableLiveData<>();

    private TallyChartRepository mRepository;

    public TallyChartViewModel(Application application) {
        super(application);
        mRepository = new TallyChartRepository();

        mStartDate = Calendar.getInstance();
        mEndDate = Calendar.getInstance();

        int currentYear = mStartDate.get(Calendar.YEAR);
        int currentMonth = mStartDate.get(Calendar.MONTH) + 1;
        Pair<Long, Long> currentMonthRange = DateUtils.monthDateRange(currentYear, currentMonth);

        // 默认显示时间为当前月
        mStartDate.setTimeInMillis(currentMonthRange.first);
        mEndDate.setTimeInMillis(currentMonthRange.second);

        init();
        refreshData();
    }

    public ObservableField<String> getCurrentDateText() {
        return mCurrentDateText;
    }

    public ObservableBoolean getDisplayDailyChart() {
        return mDisplayDailyChart;
    }

    public ObservableField<String> getExpenseTotalAmountText() {
        return mExpenseTotalAmountText;
    }

    public ObservableField<String> getIncomeTotalAmountText() {
        return mIncomeTotalAmountText;
    }

    public ObservableBoolean getDisplayExpenseChart() {
        return mDisplayExpenseChart;
    }

    LiveData<List<DailyData>> getDailyExpenseList() {
        return mObservableDailyExpenseList;
    }

    LiveData<List<DailyData>> getDailyIncomeList() {
        return mObservableDailyIncomeList;
    }

    LiveData<MonthlyDataList> getMonthlyDataList() {
        return mObservableMonthlyDataList;
    }

    LiveData<List<CategoryData>> getCategoryExpenseDataList() {
        return mObservableCategoryExpenseDataList;
    }

    LiveData<List<CategoryData>> getCategoryIncomeDataList() {
        return mObservableCategoryIncomeDataList;
    }

    /**
     * 选择日期点击
     *
     * @param activity activity
     */
    void onSelectDateClick(Activity activity) {
        Month currentMonth = new Month();
        currentMonth.setYear(mStartDate.get(Calendar.YEAR));
        currentMonth.setMonth(mStartDate.get(Calendar.MONTH) + 1);

        new MonthSelectDialog(activity, mSelectableMonthList, new MonthSelectDialog.DateSelectListener() {
            @Override
            public void onMonthSelect(MonthSelectDialog dialog, Month month) {
                dialog.dismiss();
                Pair<Long, Long> monthDateRange = DateUtils.monthDateRange(month.getYear(), month.getMonth());
                mStartDate.setTimeInMillis(monthDateRange.first);
                mEndDate.setTimeInMillis(monthDateRange.second);

                clearData();
                refreshData();
            }
        }, currentMonth).show();
    }

    /** 显示为 支出账单 点击 */
    public void onSelectAsExpenseChartClick() {
        mDisplayExpenseChart.set(true);
        refreshData();
    }

    /** 显示为 收入账单 点击 */
    public void onSelectAsIncomeChartClick() {
        mDisplayExpenseChart.set(false);
        refreshData();
    }

    /** 切换 日账单、年账单点击 */
    public void onSwitchChartModelClick() {
        if (mDisplayDailyChart.get()) {
            // 当前显示为 日账单，切换为年账单
            onSelectAsYearChartClick();
        } else {
            // 当前显示为 年账单，切换为日账单
            onSelectAsDailyChartClick();
        }
    }

    /** 显示为 年账单 点击 */
    private void onSelectAsYearChartClick() {
        mDisplayDailyChart.set(false);
        refreshData();
    }

    /** 显示为 日账单 点击 */
    private void onSelectAsDailyChartClick() {
        mDisplayDailyChart.set(true);
        refreshData();
    }

    private void init() {
        mRepository.queryFirstRecordTime(new Callback<Long, IError>() {
            @Override
            public void success(Long firstRecordTime) {
                Month firstMonth = new Month();
                Month currentMonth = new Month();

                Calendar calendar = Calendar.getInstance();
                currentMonth.setYear(calendar.get(Calendar.YEAR));
                currentMonth.setMonth(calendar.get(Calendar.MONTH) + 1);

                calendar.setTimeInMillis(firstRecordTime);
                firstMonth.setYear(calendar.get(Calendar.YEAR));
                firstMonth.setMonth(calendar.get(Calendar.MONTH) + 1);

                for (int y = firstMonth.getYear(); y <= currentMonth.getYear(); y++) {

                    for (int m = 1; m <= 12; m++) {
                        Month month = new Month();
                        month.setYear(y);
                        month.setMonth(m);
                        mSelectableMonthList.add(month);

                        if (y == currentMonth.getYear() && m == currentMonth.getMonth()) {
                            break;
                        }
                    }
                }
            }

            @Override
            public void failure(IError iError) {
                LogUtils.LOGE(TAG, "query first record time error:" + iError.toString());
            }
        });
    }

    private void clearData() {
        mDailyExpenseList.clear();
        mDailyIncomeList.clear();
        mMonthlyExpenseList.clear();
        mMonthlyIncomeList.clear();
        mCategoryDailyExpenseList.clear();
        mCategoryDailyIncomeList.clear();
        mCategoryYearlyExpenseList.clear();
        mCategoryYearlyIncomeList.clear();

        mObservableMonthlyDataList.setValue(null);
        mObservableDailyExpenseList.setValue(null);
        mObservableDailyIncomeList.setValue(null);
        mObservableCategoryExpenseDataList.setValue(null);
        mObservableCategoryIncomeDataList.setValue(null);
    }

    private void refreshData() {
        boolean isDisplayAsDaily = mDisplayDailyChart.get();
        boolean isDisplayExpense = mDisplayExpenseChart.get();

        if (isDisplayAsDaily) {
            // 查询日账单数据
            queryDailyData(v -> {
                // 显示日账单图标
                if (isDisplayExpense) {
                    mObservableDailyExpenseList.postValue(mDailyExpenseList);
                    mObservableCategoryExpenseDataList.postValue(mCategoryDailyExpenseList);
                } else {
                    mObservableDailyIncomeList.postValue(mDailyIncomeList);
                    mObservableCategoryIncomeDataList.postValue(mCategoryDailyIncomeList);
                }

                // 显示账单金额
                displayDailyExpenseAmountTotal();
                displayDailyIncomeAmountTotal();
            });
        } else {
            queryYearlyData(v -> {
                // 年账单 支出、收入 折线图
                MonthlyDataList monthlyDataList = new MonthlyDataList();
                monthlyDataList.setExpenseList(mMonthlyExpenseList);
                monthlyDataList.setIncomeList(mMonthlyIncomeList);
                mObservableMonthlyDataList.postValue(monthlyDataList);

                // 年账单 分类饼图
                if (isDisplayExpense) {
                    mObservableCategoryExpenseDataList.postValue(mCategoryYearlyExpenseList);
                } else {
                    mObservableCategoryIncomeDataList.postValue(mCategoryYearlyIncomeList);
                }

                displayMonthlyExpenseAmountTotal();
                displayMonthlyIncomeAmountTotal();
            });
        }
    }

    /***
     * 查询日账单数据
     * @param callback 回调
     */
    private void queryDailyData(SimpleCallback<Void> callback) {

        long startTime = mStartDate.getTimeInMillis();
        long endTime = mEndDate.getTimeInMillis();

        int queryModuleCount = 4;
        AtomicInteger queryCount = new AtomicInteger(0);

        // 读取日支出数据
        if (!mDailyExpenseList.isEmpty()) {
            if (queryCount.incrementAndGet() == queryModuleCount) {
                callback.success(null);
            }
        } else {
            mRepository.queryDailyExpense(startTime, endTime, new Callback<List<DailyData>, IError>() {
                @Override
                public void success(List<DailyData> dailyDataList) {
                    dailyDataList = completeEmptyDailyData(startTime, endTime, dailyDataList);
                    mDailyExpenseList.clear();
                    mDailyExpenseList.addAll(dailyDataList);

                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }

                @Override
                public void failure(IError iError) {
                    mDailyExpenseList.clear();
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }
            });
        }

        // 查询日收入数据
        if (!mDailyIncomeList.isEmpty()) {
            if (queryCount.incrementAndGet() == queryModuleCount) {
                callback.success(null);
            }
        } else {
            mRepository.queryDailyInCome(startTime, endTime, new Callback<List<DailyData>, IError>() {
                @Override
                public void success(List<DailyData> dailyDataList) {
                    dailyDataList = completeEmptyDailyData(startTime, endTime, dailyDataList);
                    mDailyIncomeList.clear();
                    mDailyIncomeList.addAll(dailyDataList);
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }

                @Override
                public void failure(IError iError) {
                    mDailyIncomeList.clear();
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }
            });
        }

        // 查询日账单支出分类数据
        if (!mCategoryDailyExpenseList.isEmpty()) {
            if (queryCount.incrementAndGet() == queryModuleCount) {
                callback.success(null);
            }
        } else {
            mRepository.queryCategoryExpense(startTime, endTime, new Callback<List<CategoryData>, IError>() {
                @Override
                public void success(List<CategoryData> categoryData) {
                    mCategoryDailyExpenseList.clear();
                    if (categoryData != null) {
                        mCategoryDailyExpenseList.addAll(categoryData);
                    }
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }

                @Override
                public void failure(IError iError) {
                    mCategoryDailyExpenseList.clear();
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }
            });
        }

        // 查询日账单收入分类数据
        if (!mCategoryDailyIncomeList.isEmpty()) {
            if (queryCount.incrementAndGet() == queryModuleCount) {
                callback.success(null);
            }
        } else {
            mRepository.queryCategoryIncome(startTime, endTime, new Callback<List<CategoryData>, IError>() {
                @Override
                public void success(List<CategoryData> categoryData) {
                    mCategoryDailyIncomeList.clear();
                    if (categoryData != null) {
                        mCategoryDailyIncomeList.addAll(categoryData);
                    }
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }

                @Override
                public void failure(IError iError) {
                    mCategoryDailyIncomeList.clear();
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }
            });
        }
    }

    /** 查询年账单数据 */
    private void queryYearlyData(SimpleCallback<Void> callback) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mStartDate.getTimeInMillis() + (mEndDate.getTimeInMillis() - mStartDate.getTimeInMillis()) / 2);
        Pair<Long, Long> yearDateRange = DateUtils.yearDateRange(calendar.get(Calendar.YEAR));
        long startTime = yearDateRange.first;
        long endTime = yearDateRange.second;

        int queryModuleCount = 4;
        AtomicInteger queryCount = new AtomicInteger(0);

        // 查询支出年账单
        if (!mMonthlyExpenseList.isEmpty()) {
            if (queryCount.incrementAndGet() == queryModuleCount) {
                callback.success(null);
            }
        } else {
            mRepository.queryMonthlyExpense(startTime, endTime, new Callback<List<MonthlyData>, IError>() {
                @Override
                public void success(List<MonthlyData> list) {
                    // 补全空数据
                    list = completeEmptyMonthlyData(startTime, endTime, list);
                    mMonthlyExpenseList.clear();
                    mMonthlyExpenseList.addAll(list);
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }

                @Override
                public void failure(IError iError) {
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }
            });
        }

        // 查询收入年账单数据
        if (!mMonthlyIncomeList.isEmpty()) {
            if (queryCount.incrementAndGet() == queryModuleCount) {
                callback.success(null);
            }
        } else {
            mRepository.queryMonthlyIncome(startTime, endTime, new Callback<List<MonthlyData>, IError>() {
                @Override
                public void success(List<MonthlyData> list) {
                    // 补全空数据
                    list = completeEmptyMonthlyData(startTime, endTime, list);
                    mMonthlyIncomeList.clear();
                    mMonthlyIncomeList.addAll(list);
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }

                @Override
                public void failure(IError iError) {
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }
            });
        }

        // 查询年账单支出分类数据
        if (!mCategoryYearlyExpenseList.isEmpty()) {
            if (queryCount.incrementAndGet() == queryModuleCount) {
                callback.success(null);
            }
        } else {
            mRepository.queryCategoryExpense(startTime, endTime, new Callback<List<CategoryData>, IError>() {
                @Override
                public void success(List<CategoryData> categoryData) {
                    mCategoryYearlyExpenseList.clear();
                    if (categoryData != null) {
                        mCategoryYearlyExpenseList.addAll(categoryData);
                    }
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }

                @Override
                public void failure(IError iError) {
                    mCategoryYearlyExpenseList.clear();
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }
            });
        }

        // 查询年账单收入分类数据
        if (!mCategoryYearlyIncomeList.isEmpty()) {
            if (queryCount.incrementAndGet() == queryModuleCount) {
                callback.success(null);
            }
        } else {
            mRepository.queryCategoryIncome(startTime, endTime, new Callback<List<CategoryData>, IError>() {
                @Override
                public void success(List<CategoryData> categoryData) {
                    mCategoryYearlyIncomeList.clear();
                    if (categoryData != null) {
                        mCategoryYearlyIncomeList.addAll(categoryData);
                    }
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }

                @Override
                public void failure(IError iError) {
                    mCategoryYearlyIncomeList.clear();
                    if (queryCount.incrementAndGet() == queryModuleCount) {
                        callback.success(null);
                    }
                }
            });
        }
    }

    /** 显示日账单支出总额 */
    private void displayDailyExpenseAmountTotal() {
        if (mDailyExpenseList == null) {
            mExpenseTotalAmountText.set(TallyUtils.formatDisplayMoney(0));
            return;
        }
        double total = 0;
        for (DailyData dailyData : mDailyExpenseList) {
            total += dailyData.getAmount();
        }
        mExpenseTotalAmountText.set(TallyUtils.formatDisplayMoney(total));
    }

    /** 显示日账单收入总额 */
    private void displayDailyIncomeAmountTotal() {
        if (mDailyIncomeList == null) {
            mIncomeTotalAmountText.set(TallyUtils.formatDisplayMoney(0));
            return;
        }
        double total = 0;
        for (DailyData dailyData : mDailyIncomeList) {
            total += dailyData.getAmount();
        }
        mIncomeTotalAmountText.set(TallyUtils.formatDisplayMoney(total));
    }

    /** 显示月账单支出总额 */
    private void displayMonthlyExpenseAmountTotal() {
        if (mMonthlyExpenseList== null) {
            mExpenseTotalAmountText.set(TallyUtils.formatDisplayMoney(0));
            return;
        }
        double total = 0;
        for (MonthlyData data : mMonthlyExpenseList) {
            total += data.getAmount();
        }
        mExpenseTotalAmountText.set(TallyUtils.formatDisplayMoney(total));
    }

    /** 显示月账单收入总额 */
    private void displayMonthlyIncomeAmountTotal() {
        if (mMonthlyIncomeList == null) {
            mIncomeTotalAmountText.set(TallyUtils.formatDisplayMoney(0));
            return;
        }
        double total = 0;
        for (MonthlyData data : mMonthlyIncomeList) {
            total += data.getAmount();
        }
        mIncomeTotalAmountText.set(TallyUtils.formatDisplayMoney(total));
    }

    /**
     * 补全起止日期内日记录数据，若有自然日没有记录，以空数据补全
     *
     * @param startDate     开始日期
     * @param endDate       结束日期
     * @param dailyDataList 源数据。
     * @return 补全后的数据集合
     */
    private List<DailyData> completeEmptyDailyData(long startDate, long endDate, List<DailyData> dailyDataList) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(startDate);
        int startYear = calendar.get(Calendar.YEAR);
        int startMonth = calendar.get(Calendar.MONTH) + 1;
        int startDay = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.setTimeInMillis(endDate);
        int endYear = calendar.get(Calendar.YEAR);
        int endMonth = calendar.get(Calendar.MONTH) + 1;
        int endDay = calendar.get(Calendar.DAY_OF_MONTH);

        // 计算总天数，不准确。大致天数为了给新集合设置初始值
        int totalDayCount = (int) ((endDate - startDate) / TimeUtils.DAY_MILLSECONDS + 2);
        // 最终补全未记录自然日的数据集合
        List<DailyData> result = new ArrayList<>(totalDayCount);

        // 遍历 起止日期的每个自然日，若自然日没有记录，以空数据补全
        // dayCursor 为查询到数据集合的遍历游标，每从集合中取一个数据到最终集合中，+1
        int dayCursor = 0;
        for (int year = startYear; year <= endYear; year++) {
            int monthStart = startYear == endYear ? startMonth : 1;
            int monthEnd = year == endYear ? endMonth : 12;
            for (int month = monthStart; month <= monthEnd; month++) {
                int monthMaxDayCount = TimeUtils.getDaysTotalOfMonth(year, month);
                for (int day = 1; day <= monthMaxDayCount; day++) {
                    DailyData nextDailyData = dailyDataList != null && dailyDataList.size() > dayCursor ? dailyDataList.get(dayCursor) : null;
                    if (nextDailyData != null
                            && nextDailyData.getYear() == year
                            && nextDailyData.getMonth() == month
                            && nextDailyData.getDayOfMonth() == day) {
                        result.add(nextDailyData);
                        dayCursor++;
                    } else {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month - 1);
                        calendar.set(Calendar.DAY_OF_MONTH, day);
                        DailyData dailyData = new DailyData();
                        dailyData.setTimeMillis(calendar.getTimeInMillis());
                        dailyData.setYear(year);
                        dailyData.setMonth(month);
                        dailyData.setDayOfMonth(day);
                        dailyData.setAmount(0);
                        result.add(dailyData);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 补全起止日期内月记录数据，若有自然月没有记录，以空数据补全
     *
     * @param startDate       开始日期
     * @param endDate         结束日期
     * @param monthlyDataList 源数据。
     * @return 补全后的数据集合
     */
    private List<MonthlyData> completeEmptyMonthlyData(long startDate, long endDate, List<MonthlyData> monthlyDataList) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(startDate);
        int startYear = calendar.get(Calendar.YEAR);
        int startMonth = calendar.get(Calendar.MONTH) + 1;

        calendar.setTimeInMillis(endDate);
        int endYear = calendar.get(Calendar.YEAR);
        int endMonth = calendar.get(Calendar.MONTH) + 1;

        // 最终补全未记录自然月的数据集合
        List<MonthlyData> result = new ArrayList<>();

        // 遍历 起止日期的每个自然月，若自然月没有记录，以空数据补全
        // dayCursor 为查询到数据集合的遍历游标，每从集合中取一个数据到最终集合中，+1
        int monthCursor = 0;
        for (int year = startYear; year <= endYear; year++) {
            int monthStart = startYear == endYear ? startMonth : 1;
            for (int month = monthStart; month <= 12; month++) {
                MonthlyData nextMonthlyData = monthlyDataList != null && monthlyDataList.size() > monthCursor ? monthlyDataList.get(monthCursor) : null;
                if (nextMonthlyData != null
                        && nextMonthlyData.getMonth() != null
                        && nextMonthlyData.getMonth().getYear() == year
                        && nextMonthlyData.getMonth().getMonth() == month) {
                    result.add(nextMonthlyData);
                    monthCursor++;
                } else {
                    MonthlyData dailyData = new MonthlyData();
                    dailyData.setMonth(new Month(year, month));
                    dailyData.setAmount(0);
                    result.add(dailyData);
                }
            }
        }

        return result;
    }

    /** 查询每日消费数据 */
    private void queryDailyExpenseData() {
        // 已经查询到数据，不再重复查询
        List<DailyData> currentData = mObservableDailyExpenseList.getValue();
        if (currentData != null) {
            mObservableDailyExpenseList.postValue(currentData);

            displayDailyExpenseAmountTotal();
            return;
        }

        long startTime = mStartDate.getTimeInMillis();
        long endTime = mEndDate.getTimeInMillis();
        mRepository.queryDailyExpense(startTime, endTime, new Callback<List<DailyData>, IError>() {
            @Override
            public void success(List<DailyData> dailyDataList) {
                dailyDataList = completeEmptyDailyData(startTime, endTime, dailyDataList);
                mObservableDailyExpenseList.postValue(dailyDataList);

                displayDailyExpenseAmountTotal();
            }

            @Override
            public void failure(IError iError) {
                showToastShort(iError.msg());
            }
        });
    }

    /** 查询每日收入数据 */
    private void queryDailyIncomeData() {
        // 已经查询到数据，不再重复查询
        List<DailyData> currentData = mObservableDailyIncomeList.getValue();
        if (currentData != null) {
            mObservableDailyIncomeList.postValue(currentData);

            displayDailyIncomeAmountTotal();
            return;
        }

        long startTime = mStartDate.getTimeInMillis();
        long endTime = mEndDate.getTimeInMillis();
        mRepository.queryDailyInCome(startTime, endTime, new Callback<List<DailyData>, IError>() {
            @Override
            public void success(List<DailyData> dailyDataList) {
                dailyDataList = completeEmptyDailyData(startTime, endTime, dailyDataList);
                mObservableDailyIncomeList.postValue(dailyDataList);

                displayDailyIncomeAmountTotal();
            }

            @Override
            public void failure(IError iError) {
                showToastShort(iError.msg());
            }
        });
    }

    /** 查询对应年-每月支出和收入数据 */
    private void queryMonthlyData() {
        // 已经查询到数据，不再重复查询
        MonthlyDataList monthlyDataList = mObservableMonthlyDataList.getValue();
        if (monthlyDataList != null && monthlyDataList.getExpenseList() != null && monthlyDataList.getIncomeList() != null) {
            mObservableMonthlyDataList.setValue(monthlyDataList);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mStartDate.getTimeInMillis() + (mEndDate.getTimeInMillis() - mStartDate.getTimeInMillis()) / 2);
        Pair<Long, Long> yearDateRange = DateUtils.yearDateRange(calendar.get(Calendar.YEAR));
        long startTime = yearDateRange.first;
        long endTime = yearDateRange.second;

        MonthlyDataList result = new MonthlyDataList();
        // 异步读取 月支出、月收入 数据，用于判断是否两部分数据都已经查询返回，全部查询返回后更新数据
        AtomicInteger counter = new AtomicInteger(0);
        // 查询支出数据
        mRepository.queryMonthlyExpense(startTime, endTime, new Callback<List<MonthlyData>, IError>() {
            @Override
            public void success(List<MonthlyData> list) {
                // 补全空数据
                list = completeEmptyMonthlyData(startTime, endTime, list);
                result.setExpenseList(list);
                if (counter.incrementAndGet() == 2) {
                    mObservableMonthlyDataList.postValue(result);

                    displayMonthlyExpenseAmountTotal();
                    displayMonthlyIncomeAmountTotal();
                }
            }

            @Override
            public void failure(IError iError) {
                showToastShort(iError.msg());
                if (counter.incrementAndGet() == 2) {
                    mObservableMonthlyDataList.postValue(result);

                    displayMonthlyExpenseAmountTotal();
                    displayMonthlyIncomeAmountTotal();
                }
            }
        });

        // 查询收入数据
        mRepository.queryMonthlyIncome(startTime, endTime, new Callback<List<MonthlyData>, IError>() {
            @Override
            public void success(List<MonthlyData> list) {
                // 补全空数据
                list = completeEmptyMonthlyData(startTime, endTime, list);
                result.setIncomeList(list);
                if (counter.incrementAndGet() == 2) {
                    mObservableMonthlyDataList.postValue(result);

                    displayMonthlyExpenseAmountTotal();
                    displayMonthlyIncomeAmountTotal();
                }
            }

            @Override
            public void failure(IError iError) {
                showToastShort(iError.msg());
                if (counter.incrementAndGet() == 2) {
                    mObservableMonthlyDataList.postValue(result);

                    displayMonthlyExpenseAmountTotal();
                    displayMonthlyIncomeAmountTotal();
                }
            }
        });
    }

    /** 查询分类支出数据 */
    private void queryCategoryExpenseData() {
        List<CategoryData> currentCategoryData = mObservableCategoryExpenseDataList.getValue();
        if (currentCategoryData != null) {
            mObservableCategoryExpenseDataList.setValue(currentCategoryData);
            return;
        }

        long startTime = mStartDate.getTimeInMillis();
        long endTime = mEndDate.getTimeInMillis();
        mRepository.queryCategoryExpense(startTime, endTime, new Callback<List<CategoryData>, IError>() {
            @Override
            public void success(List<CategoryData> categoryData) {
                mObservableCategoryExpenseDataList.postValue(categoryData);
            }

            @Override
            public void failure(IError iError) {
                showToastShort(iError.msg());
            }
        });
    }

    /** 查询分类收入数据 */
    private void queryCategoryIncomeData() {
        List<CategoryData> currentCategoryData = mObservableCategoryIncomeDataList.getValue();
        if (currentCategoryData != null) {
            mObservableCategoryIncomeDataList.setValue(currentCategoryData);
            return;
        }

        long startTime = mStartDate.getTimeInMillis();
        long endTime = mEndDate.getTimeInMillis();
        mRepository.queryCategoryIncome(startTime, endTime, new Callback<List<CategoryData>, IError>() {
            @Override
            public void success(List<CategoryData> categoryData) {
                mObservableCategoryIncomeDataList.postValue(categoryData);
            }

            @Override
            public void failure(IError iError) {
                showToastShort(iError.msg());
            }
        });
    }

}
