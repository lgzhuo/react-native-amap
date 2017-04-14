/**
 * Created by yunrui on 2017/3/7.
 */
import React, {Component, PropTypes} from 'react'
import {requireNativeComponent, View, StyleSheet, UIManager, findNodeHandle, PixelRatio} from 'react-native'
import AMapCallout from './AMapCallout'
import AMapMarker from './AMapMarker'
import AMapPolyline from './AMapPolyline'

const NativeAMap = requireNativeComponent('AMap', AMap);

export default class AMap extends Component {
    static Callout = AMapCallout;
    static Marker = AMapMarker;
    static Polyline = AMapPolyline;

    static propTypes = {
        ...View.propTypes,
        /**
         * 显示定位蓝点
         *
         */
        showsUserLocation: PropTypes.bool,
        /**
         * 显示指南针
         *
         */
        showsCompass: PropTypes.bool,
        /**
         * 显示比例尺
         *
         */
        showsScale: PropTypes.bool,
        /**
         * 显示缩放的按钮
         *
         * @platform android
         */
        showsZoomControl: PropTypes.bool,
        /**
         * 聚合点数据
         */
        clusterData: PropTypes.arrayOf(PropTypes.shape({
            latitude: PropTypes.number.isRequired,
            longitude: PropTypes.number.isRequired,
            id: PropTypes.string
        })),
        /**
         * 每个聚合点大小
         */
        clusterSize: PropTypes.number,
        /**
         * 聚合点计算完成回调
         */
        onCluster: PropTypes.func,
    };

    static defaultProps = {
        showsScale: false,
        showsCompass: false,
        showsZoomControl: false,
        clusterSize: 20
    };

    _onCluster = ({nativeEvent: {clustered}}) => {
        this.props.onCluster && this.props.onCluster(clustered)
    };

    render() {
        const {style, clusterSize, ...other} = this.props,
            props = {
                ...other,
                style: [styles.base, style]
            };

        return (
            <NativeAMap {...props} ref="map"
                        clusterSize={clusterSize * PixelRatio.get()}
                        onCluster={this._onCluster}/>
        )
    }

    centerCoordinate = (coordinate, animated = true) => {
        UIManager.dispatchViewManagerCommand(
            findNodeHandle(this),
            UIManager.AMap.Commands.centerCoordinate,
            [coordinate, animated]
        );
    };

    centerUserLocation = (animated = true) => {
        UIManager.dispatchViewManagerCommand(
            findNodeHandle(this),
            UIManager.AMap.Commands.centerUserLocation,
            [animated]
        );
    };

    fitAnnotations = (padding = 0, animated = true) => {
        UIManager.dispatchViewManagerCommand(
            findNodeHandle(this),
            UIManager.AMap.Commands.fitAnnotations,
            [padding * PixelRatio.get(), animated]
        );
    };

    fitRegion = (region, animate = true, duration = 0) => {
        UIManager.dispatchViewManagerCommand(
            findNodeHandle(this),
            UIManager.AMap.Commands.fitRegion,
            [region, animate, duration]
        );
    };

    fitCoordinates = (coordinates, edgePadding, animated = true) => {
        UIManager.dispatchViewManagerCommand(
            findNodeHandle(this),
            UIManager.AMap.Commands.fitCoordinates,
            [coordinates, edgePadding, animated]
        );
    }
}

const styles = StyleSheet.create({
    base: {
        flex: 1
    }
});