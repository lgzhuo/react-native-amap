/**
 * Created by lgzhuo on 2017/3/14.
 */
import React, {Component} from 'react'
import {requireNativeComponent, StyleSheet} from 'react-native'
import PropTypes from 'prop-types'
const NativeAMapCallout = requireNativeComponent('AMapCallout', AMapCallout);

export default class AMapCallout extends Component {
    static propTypes = {
        tooltip: PropTypes.bool
    };

    render() {
        let {style, ...props} = this.props;
        return (
            <NativeAMapCallout {...props}
                               style={[styles.callout, style]}/>
        )
    }
}

const styles = StyleSheet.create({
    callout: {
        position: 'absolute',
        backgroundColor: 'transparent'
    },
});