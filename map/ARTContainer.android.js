/**
 * Created by lgzhuo on 2017/4/12.
 */
import React, {Component} from 'react'
import {requireNativeComponent, View} from 'react-native'

const NARTContainer = requireNativeComponent('ARTContainer', ARTContainer, {
    nativeOnly: {
        nativeBackgroundAndroid: true,
        nativeForegroundAndroid: true,
    }
});

class ARTContainer extends Component {
    static propTypes = View.propTypes;
    static defaultProps = View.defaultProps;

    render() {
        return (
            <NARTContainer {...this.props}/>
        )
    }
}

module.exports = ARTContainer;