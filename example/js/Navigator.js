/**
 * Created by lgzhuo on 2017/8/10.
 */
import React from 'react'
import {StackNavigator} from 'react-navigation'
import FeatureList from './FeatureList'
import * as features from './features'

export default StackNavigator({
    FeatureList: {
        screen: FeatureList,
        navigationOptions: {
            title: 'AMapExample'
        }
    },
    ...Object.entries(features).reduce((routes, [name, screen]) => {
        routes[name] = {
            screen,
            navigationOptions: {
                title: screen.FeatureName || name
            }
        };
        return routes;
    }, {})
}, {
    initialRouteName: 'FeatureList'
})