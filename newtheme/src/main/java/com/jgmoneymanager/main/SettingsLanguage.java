package com.jgmoneymanager.main;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.widget.TextView;
import android.widget.Toast;

import com.jgmoneymanager.budget.BudgetUpdateMonthFirstDateTask;
import com.jgmoneymanager.database.DBTools;
import com.jgmoneymanager.database.MoneyManagerProvider;
import com.jgmoneymanager.database.MoneyManagerProviderMetaData;
import com.jgmoneymanager.dialogs.DialogTools;
import com.jgmoneymanager.entity.MyPreferenceActivity;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

import static com.jgmoneymanager.main.SettingsMain.languageChanged;
import static com.jgmoneymanager.main.SettingsMain.loadSettings;

/**
 * Created by Ceyhun on 21/01/2017.
 */

public class SettingsLanguage extends MyPreferenceActivity {


    ListPreference listLanguages;
    String[] languageValues = getLanguageValues();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_screen);
        addPreferencesFromResource(R.xml.language_opts);
        ((TextView)findViewById(R.id.tvATTitle)).setText(R.string.setLanguage);

        listLanguages = (ListPreference) findPreference(getResources().getString(R.string.setLanguageKey));
        listLanguages.setEntryValues(languageValues);
        listLanguages.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Tools.loadLanguage(SettingsLanguage.this, newValue.toString());
                Tools.resetFormats(SettingsLanguage.this, newValue.toString());
                DBTools.execQuery(SettingsLanguage.this, "Drop view if exists " + MoneyManagerProviderMetaData.VTransAccountViewMetaData.VIEW_NAME);
                DBTools.execQuery(SettingsLanguage.this,
                        MoneyManagerProvider.DatabaseHelper.DATABASE_CREATE_VIEW_VTRANSACCOUNTS.replace("'ALL'",
                                "'" + getResources().getString(R.string.all) + "'"));
                DBTools.execQuery(SettingsLanguage.this, "Drop view if exists " + MoneyManagerProviderMetaData.VTransferViewMetaData.VIEW_NAME);
                DBTools.execQuery(SettingsLanguage.this,
                        MoneyManagerProvider.DatabaseHelper.DATABASE_CREATE_VIEW_VTRANSFER.
                                replace("income", getResources().getString(R.string.income).toLowerCase()).
                                replace("expense", getResources().getString(R.string.expense).toLowerCase()));
                languageChanged = true;
                restartActivity();
                return true;
            }
        });

        Preference btResetButton = (Preference) findPreference(getString(R.string.setResetLocalizationKey));
        btResetButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Tools.resetFormats(SettingsLanguage.this);
                languageChanged = true;
                restartActivity();
                return true;
            }
        });

        final ListPreference listDateFormat = (ListPreference) findPreference(getResources().getString(R.string.setDateFormatKey));
        listDateFormat.setSummary(getDateFormatSummary(null));
        listDateFormat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                listDateFormat.setSummary(getDateFormatSummary((String)newValue));
                loadSettings = true;
                return true;
            }
        });

        final ListPreference listWeekFirstDay = (ListPreference) findPreference(getResources().getString(R.string.setWeekFirstDayKey));
        listWeekFirstDay.setSummary(getFirstDayOfWeekSummary(null));
        listWeekFirstDay.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                listWeekFirstDay.setSummary(getFirstDayOfWeekSummary((String)newValue));
                loadSettings = true;
                return true;
            }
        });

        final ListPreference listFirstMonthDateDay = (ListPreference) findPreference(getResources().getString(R.string.setMonthFirstDateKey));
        listFirstMonthDateDay.setSummary(getFirstDateOfMonthSummary(null));
        listFirstMonthDateDay.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                listFirstMonthDateDay.setSummary(getFirstDateOfMonthSummary((String)newValue));
                loadSettings = true;
                if (Constants.MonthFirstDate != Integer.parseInt((String) newValue)) {
                    BudgetUpdateMonthFirstDateTask budgetUpdateMonthFirstDateTask = new BudgetUpdateMonthFirstDateTask(SettingsLanguage.this, (String) newValue);
                    budgetUpdateMonthFirstDateTask.execute();
                }
                return true;
            }
        });

        /*final ListPreference listFirstWeekDay = (ListPreference) findPreference(getResources().getString(R.string.setFirstWeekDayKey));
        listFirstWeekDay.setSummary(getFirstDayOfWeekSummary(null));
        listFirstWeekDay.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                listFirstWeekDay.setSummary(getFirstDayOfWeekSummary((String)newValue));
                loadSettings = true;
                return true;
            }
        });*/

        final ListPreference curSignPosition = (ListPreference) findPreference(getResources().getString(R.string.setCurSignKey));
        curSignPosition.setSummary(getDecimalSymbolPositionSummary(null));
        curSignPosition.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                curSignPosition.setSummary(getDecimalSymbolPositionSummary(newValue.toString()));
                loadSettings = true;
                return true;
            }
        });

        final ListPreference decimalSymbol = (ListPreference) findPreference(getResources().getString(R.string.setDecimalSymbolKey));
        decimalSymbol.setSummary(Tools.getPreference(SettingsLanguage.this, R.string.setDecimalSymbolKey, R.string.setDotKey));
        decimalSymbol.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.toString().equals(Tools.getPreference(SettingsLanguage.this, R.string.setDigitsGroupingSymbolKey, R.string.setNoneKey))) {
                    DialogTools.toastDialog(SettingsLanguage.this, R.string.msgSameDecimalAndDigitSymbol, Toast.LENGTH_LONG);
                    return false;
                }
                else {
                    decimalSymbol.setSummary((String) newValue);
                    loadSettings = true;
                    return true;
                }
            }
        });

        final ListPreference decimalDigitsCount = (ListPreference) findPreference(getResources().getString(R.string.setDecimalDigitsCountKey));
        decimalDigitsCount.setSummary(Tools.getPreference(SettingsLanguage.this, R.string.setDecimalDigitsCountKey, R.string.setDecimalDigitsCountDefaultKey));
        decimalDigitsCount.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                decimalDigitsCount.setSummary((String)newValue);
                loadSettings = true;
                return true;
            }
        });

        final ListPreference decimalDigitsCountCurrency = (ListPreference) findPreference(getResources().getString(R.string.setDecimalDigitsCountCurrencyKey));
        decimalDigitsCountCurrency.setSummary(Tools.getPreference(SettingsLanguage.this, R.string.setDecimalDigitsCountCurrencyKey, R.string.setDecimalDigitsCountCurrencyDefaultKey));
        decimalDigitsCountCurrency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                decimalDigitsCountCurrency.setSummary((String)newValue);
                loadSettings = true;
                return true;
            }
        });

        final ListPreference digitsGroupingSymbol = (ListPreference) findPreference(getResources().getString(R.string.setDigitsGroupingSymbolKey));
        digitsGroupingSymbol.setSummary(getDigitsGroupingSymbolSummary(null));
        digitsGroupingSymbol.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.toString().equals(Tools.getPreference(SettingsLanguage.this, R.string.setDecimalSymbolKey, R.string.setNoneKey))) {
                    DialogTools.toastDialog(SettingsLanguage.this, R.string.msgSameDecimalAndDigitSymbol, Toast.LENGTH_LONG);
                    return false;
                }
                else {
                    digitsGroupingSymbol.setSummary(getDigitsGroupingSymbolSummary(newValue.toString()));
                    loadSettings = true;
                    return true;
                }
            }
        });
    }


    String[] getLanguageValues() {
        String[] values = new String[Constants.LanguageValues.values().length];
        int i = 0;
        for (Constants.LanguageValues value : Constants.LanguageValues.values()) {
            values[i] = Constants.LanguageValues.getValue(value.index());
            i++;
        }
        return values;
    }

    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    /**
     * If newValue exists, returns according to it, else reads current preference
     * @param newValue
     * @return
     */
    int getDecimalSymbolPositionSummary(String newValue) {
        String value;
        if (newValue != null)
            value = newValue;
        else
            value = Tools.getPreference(SettingsLanguage.this, R.string.setCurSignKey, R.string.setCurSignLeftKey);
        if (value.equals(getResources().getString(R.string.setCurSignLeftKey)))
            return R.string.left;
        else return R.string.right;
    }

    /**
     * If newValue exists, returns according to it, else reads current preference
     * @param newValue
     * @return
     */
    String getDigitsGroupingSymbolSummary(String newValue) {
        String value;
        if (newValue != null)
            value = newValue;
        else
            value = Tools.getPreference(SettingsLanguage.this, R.string.setDigitsGroupingSymbolKey, R.string.setNoneKey);
        if (value.equals(getResources().getString(R.string.setNoneKey)))
            return getString(R.string.none);
        else return value;
    }

    /*String getFirstDayOfWeekSummary(String newValue) {
        String value;
        String result;
        if (newValue != null)
            value = newValue;
        else
            value = Tools.getPreference(SettingsLanguage.this, R.string.setFirstWeekDayKey, R.string.setMondayKey);
        return Tools.getValueFromKeyArray(this, R.array.weeksArrayKey, R.array.weeksArray, value);
    }*/

    String getDateFormatSummary(String newValue) {
        String value;
        if (newValue != null)
            value = newValue;
        else
            value = Tools.getPreference(SettingsLanguage.this, R.string.setDateFormatKey, R.string.dfk_ddmmyyyy_point);
        return Tools.getValueFromKeyArray(SettingsLanguage.this, R.array.dateFormatTypeKeysArray, R.array.dateFormatTypeArray, value);
    }

    String getFirstDayOfWeekSummary(String newValue) {
        String value;
        if (newValue != null)
            value = newValue;
        else
            value = Tools.getPreference(SettingsLanguage.this, R.string.setWeekFirstDayKey, R.string.setMondayKey);
        return Tools.getValueFromKeyArray(SettingsLanguage.this, R.array.weeksArrayKey, R.array.weeksArray, value);
    }

    String getFirstDateOfMonthSummary(String newValue) {
        String value;
        if (newValue != null)
            value = newValue;
        else
            value = Tools.getPreference(SettingsLanguage.this, R.string.setMonthFirstDateKey, R.string.digitCountValue1);
        return Tools.getValueFromKeyArray(SettingsLanguage.this, R.array.monthValueArray, R.array.monthValueArray, value);
    }

}
