import React, { useEffect, useRef, useState, useCallback } from 'react';
import {
    createChart,
    CrosshairMode,
    CandlestickSeries,
    HistogramSeries,
    LineSeries,
    ColorType,
} from 'lightweight-charts';
import classes from './CandleChart.module.css';

const KRW = (v) => v == null ? '-' : v.toLocaleString('ko-KR');

/** 단순 이동평균 계산 */
function calcMA(candles, period) {
    const result = [];
    for (let i = 0; i < candles.length; i++) {
        if (i < period - 1) continue;
        let sum = 0;
        for (let j = i - period + 1; j <= i; j++) sum += candles[j].close;
        result.push({ time: candles[i].date, value: sum / period });
    }
    return result;
}

const MA_CONFIG = [
    { period: 5, color: '#EAB308', label: 'MA5' },
    { period: 20, color: '#EF4444', label: 'MA20' },
    { period: 60, color: '#3B82F6', label: 'MA60' },
];

const CandleChart = ({ candles }) => {
    const containerRef = useRef(null);
    const chartRef = useRef(null);
    const candleSeriesRef = useRef(null);
    const [info, setInfo] = useState(null);
    const [maVisible, setMaVisible] = useState({ 5: true, 20: true, 60: true });
    const maSeriesRef = useRef({});

    const lastCandle = candles?.[candles.length - 1];

    const handleCrosshairMove = useCallback((param) => {
        if (!param?.time || !candleSeriesRef.current) {
            setInfo(null);
            return;
        }
        const data = param.seriesData?.get(candleSeriesRef.current);
        if (data) {
            setInfo({
                date: param.time,
                open: data.open,
                high: data.high,
                low: data.low,
                close: data.close,
                change: data.close - data.open,
                changeRate: data.open ? ((data.close - data.open) / data.open * 100) : 0,
            });
        }
    }, []);

    useEffect(() => {
        if (!containerRef.current || !candles || candles.length === 0) return;

        if (chartRef.current) {
            chartRef.current.remove();
            chartRef.current = null;
        }

        const chart = createChart(containerRef.current, {
            width: containerRef.current.clientWidth,
            height: containerRef.current.clientHeight || 400,
            layout: {
                background: { type: ColorType.Solid, color: '#FFFDF7' },
                textColor: '#A89968',
                fontSize: 11,
                fontFamily: "'Pretendard Variable', -apple-system, sans-serif",
            },
            grid: {
                vertLines: { color: 'rgba(160,130,50,0.06)' },
                horzLines: { color: 'rgba(160,130,50,0.06)' },
            },
            crosshair: {
                mode: CrosshairMode.Normal,
                vertLine: {
                    color: 'rgba(234,179,8,0.4)',
                    style: 0,
                    width: 1,
                    labelBackgroundColor: '#EAB308',
                },
                horzLine: {
                    color: 'rgba(234,179,8,0.4)',
                    style: 0,
                    width: 1,
                    labelBackgroundColor: '#EAB308',
                },
            },
            rightPriceScale: {
                borderColor: 'rgba(160,130,50,0.12)',
                scaleMargins: { top: 0.06, bottom: 0.24 },
                entireTextOnly: true,
            },
            timeScale: {
                borderColor: 'rgba(160,130,50,0.12)',
                timeVisible: true,
                secondsVisible: false,
                rightOffset: 4,
                minBarSpacing: 3,
            },
            localization: {
                priceFormatter: (p) => KRW(Math.round(p)),
            },
        });

        // --- 캔들스틱 ---
        const candleSeries = chart.addSeries(CandlestickSeries, {
            upColor: '#EF4444',
            downColor: '#3B82F6',
            borderUpColor: '#DC2626',
            borderDownColor: '#2563EB',
            wickUpColor: '#DC2626',
            wickDownColor: '#2563EB',
        });
        const candleData = candles.map(c => ({
            time: c.date,
            open: c.open,
            high: c.high,
            low: c.low,
            close: c.close,
        }));
        candleSeries.setData(candleData);
        candleSeriesRef.current = candleSeries;

        // --- 이동평균선 ---
        const newMaSeries = {};
        MA_CONFIG.forEach(({ period, color }) => {
            const maData = calcMA(candles, period);
            if (maData.length < 2) return;
            const series = chart.addSeries(LineSeries, {
                color,
                lineWidth: 1,
                crosshairMarkerVisible: false,
                priceLineVisible: false,
                lastValueVisible: false,
                visible: maVisible[period],
            });
            series.setData(maData);
            newMaSeries[period] = series;
        });
        maSeriesRef.current = newMaSeries;

        // --- 거래량 ---
        const volumeSeries = chart.addSeries(HistogramSeries, {
            priceFormat: { type: 'volume' },
            priceScaleId: 'vol',
        });
        chart.priceScale('vol').applyOptions({
            scaleMargins: { top: 0.80, bottom: 0 },
        });
        volumeSeries.setData(
            candles.map(c => ({
                time: c.date,
                value: c.volume,
                color: c.close >= c.open
                    ? 'rgba(239,68,68,0.20)'
                    : 'rgba(59,130,246,0.20)',
            }))
        );

        chart.timeScale().fitContent();
        chart.subscribeCrosshairMove(handleCrosshairMove);
        chartRef.current = chart;

        const ro = new ResizeObserver(() => {
            if (containerRef.current && chartRef.current) {
                chartRef.current.applyOptions({
                    width: containerRef.current.clientWidth,
                });
            }
        });
        ro.observe(containerRef.current);

        return () => {
            ro.disconnect();
            chart.unsubscribeCrosshairMove(handleCrosshairMove);
            if (chartRef.current) {
                chartRef.current.remove();
                chartRef.current = null;
            }
        };
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [candles, handleCrosshairMove]);

    // MA 토글
    const toggleMA = (period) => {
        setMaVisible(prev => {
            const next = { ...prev, [period]: !prev[period] };
            const series = maSeriesRef.current[period];
            if (series) {
                series.applyOptions({ visible: next[period] });
            }
            return next;
        });
    };

    const display = info || (lastCandle ? {
        date: lastCandle.date,
        open: lastCandle.open,
        high: lastCandle.high,
        low: lastCandle.low,
        close: lastCandle.close,
        change: lastCandle.close - lastCandle.open,
        changeRate: lastCandle.open ? ((lastCandle.close - lastCandle.open) / lastCandle.open * 100) : 0,
    } : null);

    const isUp = display && display.change >= 0;

    return (
        <div className={classes.wrapper}>
            {/* OHLCV 정보 바 */}
            {display && (
                <div className={classes.infoBar}>
                    <div className={classes.infoLeft}>
                        <span className={classes.infoLabel}>시</span>
                        <span className={classes.infoValue}>{KRW(display.open)}</span>
                        <span className={classes.infoLabel}>고</span>
                        <span className={`${classes.infoValue} ${classes.rise}`}>{KRW(display.high)}</span>
                        <span className={classes.infoLabel}>저</span>
                        <span className={`${classes.infoValue} ${classes.fall}`}>{KRW(display.low)}</span>
                        <span className={classes.infoLabel}>종</span>
                        <span className={`${classes.infoValue} ${isUp ? classes.rise : classes.fall}`}>
                            {KRW(display.close)}
                        </span>
                        <span className={`${classes.infoChange} ${isUp ? classes.rise : classes.fall}`}>
                            {isUp ? '+' : ''}{display.changeRate.toFixed(2)}%
                        </span>
                    </div>
                    <div className={classes.maToggles}>
                        {MA_CONFIG.map(({ period, color, label }) => (
                            <button
                                key={period}
                                className={`${classes.maBtn} ${maVisible[period] ? '' : classes.maBtnOff}`}
                                style={{ '--ma-color': color }}
                                onClick={() => toggleMA(period)}
                            >
                                {label}
                            </button>
                        ))}
                    </div>
                </div>
            )}
            <div ref={containerRef} className={classes.chart} />
        </div>
    );
};

export default CandleChart;
