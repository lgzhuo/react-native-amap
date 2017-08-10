/**
 * Created by lgzhuo on 2017/3/16.
 */
import React, {Component} from 'react'
import {requireNativeComponent} from 'react-native'
import PropTypes from 'prop-types'
const NativeAMapPolyline = requireNativeComponent('AMapPolyline', AMapPolyline);

export default class AMapPolyline extends Component {
    static propTypes = {
        /**
         * 坐标点列表
         */
        coordinates: PropTypes.arrayOf(PropTypes.shape({
            latitude: PropTypes.number.isRequired,
            longitude: PropTypes.number.isRequired,
        })).isRequired,

        /**
         * 点击时触发
         */
        onPress: PropTypes.func,

        /**
         * 填充颜色
         */
        fillColor: PropTypes.string,

        /**
         * 笔触宽度
         */
        strokeWidth: PropTypes.number,

        /**
         * 笔触颜色
         */
        strokeColor: PropTypes.string,

        /**
         * z轴值
         *
         * @platform android
         */
        zIndex: PropTypes.number,

        /**
         * LineCap,默认是 `butt`.
         *
         * @platform ios
         */
        lineCap: PropTypes.oneOf([
            'butt',
            'arrow',
            'round',
            'square',
        ]),

        /**
         * LineJoin,默认是 `bevel`
         *
         * @platform ios
         */
        lineJoin: PropTypes.oneOf([
            'bevel',
            'miter',
            'round',
        ]),

        /**
         * MiterLimit,默认是10.f.
         *
         * @platform ios
         */
        miterLimit: PropTypes.number,

        /**
         * 是否画大地曲线，默认false
         *
         * @platform android
         */
        geodesic: PropTypes.bool,

        /**
         * 是否绘制成虚线, 默认是NO
         *
         */
        lineDash: PropTypes.bool,
    };

    static defaultProps = {
        strokeColor: '#000',
        strokeWidth: 1,
    };

    render() {
        return (
            <NativeAMapPolyline {...this.props}/>
        )
    }
}