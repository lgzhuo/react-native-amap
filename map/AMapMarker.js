/**
 * Created by lgzhuo on 2017/3/9.
 */
import React, {Component} from 'react'
import {requireNativeComponent, StyleSheet, UIManager, findNodeHandle} from 'react-native'
import PropTypes from 'prop-types'
import resolveAssetSource from 'react-native/Libraries/Image/resolveAssetSource';
const NativeAMapMarker = requireNativeComponent('AMapMarker', AMapMarker, {
    nativeOnly: {
        onSelect: true
    }
});

export default class AMapMarker extends Component {
    static propTypes = {
        /**
         * Marker覆盖物的锚点比例。 锚点是定位图标接触地图平面的点。
         * 图标的左上顶点为（0,0）点，右下点为（1,1）点。
         * 默认情况下，锚点为（0.5,1.0）。
         *
         * @platform android
         */
        anchor: PropTypes.shape({
            u: PropTypes.number.isRequired,
            v: PropTypes.number.isRequired,
        }),
        /**
         * 是否支持拖动
         *
         */
        draggable: PropTypes.bool,
        /**
         * 设置 Marker 覆盖物的位置坐标。
         *
         */
        coordinate: PropTypes.shape({
            latitude: PropTypes.number.isRequired,
            longitude: PropTypes.number.isRequired,
        }).isRequired,
        /**
         * Marker覆盖物的图片旋转角度，从正北开始，逆时针计算。
         *
         * @platform android
         */
        rotateAngle: PropTypes.number,
        /**
         * Marker覆盖物是否平贴地图。
         * 参数:
         * 平贴地图设置为 true，面对镜头设置为 false。
         *
         * @platform android
         */
        flat: PropTypes.bool,
        /**
         * 设置Marker覆盖物的坐标是否是Gps，默认为false。
         *
         * @platform android
         */
        gps: PropTypes.bool,
        /**
         * Marker覆盖物的InfoWindow相对Marker的偏移。
         * 坐标系原点为marker的中上点，InfoWindow相对此原点的像素偏移，向左和向上上为负，向右和向下为正。InfoWindow的初始位置为marker上边线与InfoWindow下边线重合，并且两者的中线在一条线上。
         * 参数:
         * x - callOut相对原点的横向像素偏移量，单位：像素。
         * y - callOut相对原点的纵向像素偏移量，单位：像素。
         *
         */
        calloutOffset: PropTypes.shape({
            x: PropTypes.number,
            y: PropTypes.number,
        }),
        /**
         * Marker 覆盖物的文字片段。
         *
         */
        description: PropTypes.string,
        /**
         * Marker 覆盖物的标题。
         *
         */
        title: PropTypes.string,
        /**
         * Marker覆盖物是否可见。
         *
         * @platform android
         */
        visible: PropTypes.bool,
        /**
         * 设置Marker覆盖物 zIndex。
         *
         */
        zIndex: PropTypes.number,
        /**
         * Marker 的图标，本地图片或网络图片的地址
         *
         */
        image: PropTypes.any,
        /**
         * 默认大头针的颜色
         *
         */
        pinColor: PropTypes.string,
        /**
         * 是否允许弹出气泡窗
         *
         */
        calloutEnabled: PropTypes.bool,
        /**
         * 选中状态改变时触发
         *
         * @platform ios
         */
        onSelectChange: PropTypes.func,
        /**
         * Marker 点击时触发
         *
         */
        onPress: PropTypes.func,
        /**
         * 气泡框点击时触发
         *
         */
        onCalloutPress: PropTypes.func,
    };

    static defaultProps = {
        calloutEnabled: true,
    };

    _onSelect = event => {
        this.props.onSelect && this.props.onSelect(event);
        this.props.onSelectChange && this.props.onSelectChange(event.nativeEvent.selected);
    };

    render() {
        let {style, image, ...props} = this.props;
        if (image) {
            image = (resolveAssetSource(image) || {}).uri;
        }
        return (
            <NativeAMapMarker {...props}
                              onSelect={this._onSelect}
                              image={image}
                              style={[styles.marker, style]}/>
        )
    }

    _runCommand = (command, ...props) => {
        UIManager.dispatchViewManagerCommand(
            findNodeHandle(this),
            UIManager.AMapMarker.Commands.showCallout,
            props
        );
    };

    showCallout = () => this._runCommand('showCallout');

    hideCallout = () => this._runCommand('hideCallout');
}

const styles = StyleSheet.create({
    marker: {
        position: 'absolute',
        backgroundColor: 'transparent',
    },
});