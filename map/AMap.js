/**
 * Created by lgzhuo on 2017/3/7.
 */
import React, {Component, PropTypes} from 'react'
import {requireNativeComponent, View, StyleSheet, UIManager, findNodeHandle, PixelRatio, Platform} from 'react-native'
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
        /**
         * 在marker被点击时是否移动地图将marker居中
         * Default value is `true`
         *
         * @platform android
         */
        moveOnMarkerPress: PropTypes.bool,

        /**
         * The region to be displayed by the map.
         *
         * The region is defined by the center coordinates and the span of
         * coordinates to display.
         */
        region: PropTypes.shape({
            /**
             * Coordinates for the center of the map.
             */
            latitude: PropTypes.number.isRequired,
            longitude: PropTypes.number.isRequired,

            /**
             * Difference between the minimun and the maximum latitude/longitude
             * to be displayed.
             */
            latitudeDelta: PropTypes.number.isRequired,
            longitudeDelta: PropTypes.number.isRequired,
        }),

        /**
         * The initial region to be displayed by the map.  Use this prop instead of `region`
         * only if you don't want to control the viewport of the map besides the initial region.
         *
         * Changing this prop after the component has mounted will not result in a region change.
         *
         * This is similar to the `initialValue` prop of a text input.
         */
        initialRegion: PropTypes.shape({
            /**
             * Coordinates for the center of the map.
             */
            latitude: PropTypes.number.isRequired,
            longitude: PropTypes.number.isRequired,

            /**
             * Difference between the minimun and the maximum latitude/longitude
             * to be displayed.
             */
            latitudeDelta: PropTypes.number.isRequired,
            longitudeDelta: PropTypes.number.isRequired,
        }),
    };

    static defaultProps = {
        showsScale: false,
        showsCompass: false,
        showsZoomControl: false,
        clusterSize: 20,
        moveOnMarkerPress: true
    };

    constructor(props, context) {
        super(props, context);
        this.state = {
            isReady: Platform.OS === 'ios',
        };
    }

    componentWillUpdate(nextProps) {
        if (nextProps.region) {
            this.map.setNativeProps({region: nextProps.region});
        }
    }

    render() {
        let {style, clusterSize, ...props} = this.props;

        if (this.state.isReady) {
            props = {
                ...props,
                region: undefined,
                initialRegion: undefined,
                onMapReady: this._onMapReady,
                clusterSize: clusterSize * PixelRatio.get(),
                onCluster: this._onCluster,
                onLayout: this._onLayout,
            };
        } else {
            props = {
                region: undefined,
                initialRegion: undefined,
                onMapReady: this._onMapReady,
                onLayout: this._onLayout,
            };
        }

        return (
            <NativeAMap {...props} ref={ref => this.map = ref}
                        style={[styles.base, style]}/>
        )
    }

    _onCluster = ({nativeEvent: {clustered}}) => {
        this.props.onCluster && this.props.onCluster(clustered)
    };

    _onMapReady = () => {
        const {region, initialRegion} = this.props;
        if (region) {
            this.map.setNativeProps({region});
        } else if (initialRegion) {
            this.map.setNativeProps({region: initialRegion});
        }
        this.setState({isReady: true});
    };

    _onLayout = e => {
        const {layout} = e.nativeEvent;
        if (!layout.width || !layout.height) return;
        if (this.state.isReady && !this.__layoutCalled) {
            const region = this.props.region || this.props.initialRegion;
            if (region) {
                this.__layoutCalled = true;
                this.map.setNativeProps({region});
            }
        }
        if (this.props.onLayout) {
            this.props.onLayout(e);
        }
    };

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
        switch (typeof edgePadding) {
            case 'object':
                const {top = 0, left = 0, right = 0, bottom = 0} = edgePadding,
                    pr = PixelRatio.get();
                edgePadding = {
                    top: top * pr,
                    left: left * pr,
                    bottom: bottom * pr,
                    right: right * pr
                };
                break;
            case 'number':
                const ep = edgePadding * PixelRatio.get();
                edgePadding = {
                    top: ep,
                    left: ep,
                    bottom: ep,
                    right: ep
                };
                break;
            default:
                edgePadding = undefined;
                break;
        }
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
