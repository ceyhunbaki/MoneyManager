package com.jgmoneymanager.main;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.jgmoneymanager.entity.MyActivity;
import com.jgmoneymanager.tools.Constants;
import com.jgmoneymanager.tools.Tools;

public class Calculator extends MyActivity{
	
	Double sumAmount;	
	double oldValue;
    String historyText = "";
	//boolean doNothing = false;
	boolean clearAll = true;
	
	public static final String startupAmountKey = "startupAmountKey";
	
	final int opNone = 0;
	final int opDivide = 1;
	final int opSubtract = 2;
	final int opMinus = 3;
	final int opPlus = 4;
	int operation = opNone;

	final int symbolNone = 0;
	final int symbolNumber = 1;
	final int symbolSign = 2;
	final int symbolEqual = 3;
	int previousSymbol = symbolNone;

	TextView txConsole;
	
	final String dot = ".";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calculator);	
		
		this.setTitle(R.string.calculator);
		txConsole = (TextView) findViewById(R.id.clConsole);
		
		if (savedInstanceState == null) {
			clearAll();
			if (getIntent().getExtras() != null) {
				sumAmount = getIntent().getExtras().getDouble(startupAmountKey);
				//doNothing = false;
				previousSymbol = symbolEqual;
			}
		}
		else {
			sumAmount = Tools.getDoubleFromBundle(savedInstanceState, "sumAmount");
			oldValue = Tools.getDoubleFromBundle0(savedInstanceState, "oldValue");
			operation = Tools.getIntegerFromBundle0(savedInstanceState, "operation");
			previousSymbol = Tools.getIntegerFromBundle0(savedInstanceState, "previousSymbol");
			//doNothing = Tools.getBooleanFromBundle(savedInstanceState, "doNothing");
			clearAll = Tools.getBooleanFromBundle0(savedInstanceState, "clearAll");
            historyText = Tools.getStringFromBundle(savedInstanceState, "historyText");
		}

		reloadScreen();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
        Tools.putToBundle(outState, "sumAmount", sumAmount);
        Tools.putToBundle(outState, "historyText", historyText);
		Tools.putToBundle(outState, "oldValue", oldValue);
		Tools.putToBundle(outState, "operation", operation);
		Tools.putToBundle(outState, "previousSymbol", previousSymbol);
		//Tools.putToBundle(outState, "doNothing", doNothing);
		Tools.putToBundle(outState, "clearAll", clearAll);
		super.onSaveInstanceState(outState);
	}

	void clearAll() {
		sumAmount = 0d;
		oldValue = 0d;
		operation = opNone;
		//doNothing = false;
		txConsole.setText("0");
		clearAll = true;
		previousSymbol = symbolNone;
        historyText = "";
        ((TextView)findViewById(R.id.clConsoleHistory)).setText(historyText);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	    setContentView(R.layout.calculator);
	    reloadScreen();
	}
	
	private void reloadScreen() {
		txConsole.setText(String.valueOf(sumAmount));
        ((TextView)findViewById(R.id.clConsoleHistory)).setText(historyText);
	}

	void appendNumbers(String text) {
		if (previousSymbol == symbolSign)
			if (text.equals(dot))
				txConsole.setText("0.");
			else
				txConsole.setText(text);
		else if (previousSymbol == symbolNumber) 
			txConsole.append(text);
		else {
			operation = opNone;
			oldValue = 0d;
			if (text.equals(dot))
				txConsole.setText("0.");
			else
				txConsole.setText(text);
		}
		sumAmount = Tools.parseDouble(txConsole.getText().toString());
		//doNothing = false;
		previousSymbol = symbolNumber;
	}
	
	void appendSign(int opType, String sign) {
		if (previousSymbol == symbolNumber) {
            historyText = historyText + String.valueOf(sumAmount) + sign;
			if (operation != opNone)
				doOperation(false);
			oldValue = sumAmount;
			previousSymbol = symbolSign;
			operation = opType;
		}
		else if (previousSymbol == symbolSign) {
            operation = opType;
            if (historyText.length() == 0)
                historyText = "00";
            historyText = historyText.substring(0, historyText.length()-1) + sign;
        }
		else if (previousSymbol == symbolEqual) {
			oldValue = sumAmount;
			operation = opType;
            historyText = String.valueOf(sumAmount) + sign;
		}
		previousSymbol = symbolSign;
        ((TextView)findViewById(R.id.clConsoleHistory)).setText(historyText);
	}
	
	void deleteSymbol() {
        if (previousSymbol != symbolSign) {
            String value = txConsole.getText().toString();
            if (value.length() == 1) {
                txConsole.setText("0");
                previousSymbol = symbolSign;
                /*previousSymbol = symbolNone;
                sumAmount = 0d;
                if (value.equals("0"))
                    operation = opNone;*/
            } else {
                value = value.substring(0, value.length() - 1);
                if (value.indexOf(".") == value.length() - 1)
                    sumAmount = Tools.parseDouble(value.substring(0, value.length() - 1));
                else
                try {
                    sumAmount = Double.parseDouble(value);
                }
                catch (NumberFormatException e){
                    sumAmount = 0d;
                }
                txConsole.setText(value);
            }
        }
	}

    void doOperation(boolean changeHistoryText) {
        double newValue = sumAmount;
        switch (operation) {
            case opDivide:
                if (previousSymbol != symbolEqual)
                    sumAmount = oldValue / sumAmount;
                else
                    sumAmount = sumAmount / oldValue;
                break;
            case opSubtract:
                sumAmount = oldValue * sumAmount;
                break;
            case opMinus:
                if (previousSymbol != symbolEqual)
                    sumAmount = oldValue - sumAmount;
                else
                    sumAmount = sumAmount - oldValue;
                break;
            case opPlus:
                sumAmount = oldValue + sumAmount;
                break;
            default:
                break;
        }
        txConsole.setText(sumAmount.toString());
        if (previousSymbol != symbolEqual)
            oldValue = newValue;
        if (changeHistoryText) {
            historyText = "";
            ((TextView) findViewById(R.id.clConsoleHistory)).setText(historyText);
        }
    }

    public void myClickHandler(View target) {
        switch (target.getId()) {
            case R.id.clOne:
                appendNumbers("1");
                break;
            case R.id.clTwo:
                appendNumbers("2");
                break;
            case R.id.clThree:
                appendNumbers("3");
                break;
            case R.id.clFour:
                appendNumbers("4");
                break;
            case R.id.clFive:
                appendNumbers("5");
                break;
            case R.id.clSix:
                appendNumbers("6");
                break;
            case R.id.clSeven:
                appendNumbers("7");
                break;
            case R.id.clEight:
                appendNumbers("8");
                break;
            case R.id.clNine:
                appendNumbers("9");
                break;
            case R.id.clZero:
                if (!txConsole.getText().toString().equals("0"))
                    appendNumbers("0");
                break;
            case R.id.clDot:
                if (!txConsole.getText().toString().contains("."))
                    appendNumbers(dot);
                break;
            case R.id.clSign:
                sumAmount = -sumAmount;
                if (sumAmount < 0)
                    txConsole.setText("-"+txConsole.getText());
                else if (sumAmount > 0)
                    txConsole.setText(txConsole.getText().toString().substring(1));
                break;
            case R.id.clDivide:
                appendSign(opDivide, "/");
                break;
            case R.id.clSubtract:
                appendSign(opSubtract, "*");
                break;
            case R.id.clMinus:
                appendSign(opMinus, "-");
                break;
            case R.id.clAdd:
                appendSign(opPlus, "+");
                break;
            case R.id.clEqual:
                doOperation(true);
                //doNothing = true;
                //operation = opNone;
                previousSymbol = symbolEqual;
                break;
            case R.id.clCE:
                clearAll();
                break;
            case R.id.clBack:
                deleteSymbol();
                //doNothing = false;
                break;
            case R.id.clOK:
                //doNothing = false;
                Intent intent = new Intent();
                intent.putExtra(Constants.calculatorValue, sumAmount);
                setResult(RESULT_OK, intent);
                finish();
                break;
            case R.id.clCancel:
                //doNothing = false;
                setResult(RESULT_CANCELED);
                finish();
                break;
            default:
                break;
        }
    }
}
