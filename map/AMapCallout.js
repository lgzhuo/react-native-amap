/**
 * Created by yunrui on 2017/3/14.
 */
import React, {Component} from 'react'
import {requireNativeComponent, StyleSheet} from 'react-native'
const NativeAMapCallout = requireNativeComponent('AMapCallout', AMapCallout);

export default class AMapCallout extends Component {
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