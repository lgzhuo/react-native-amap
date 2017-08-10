/**
 * Created by lgzhuo on 2017/8/10.
 */
import React from 'react'
import {StyleSheet, View, FlatList, TouchableOpacity, Text} from 'react-native'
import * as features from './features'

const featureData = Object.entries(features).map(([name, component]) => ({
    key: name,
    title: component.FeatureName || name
}));

export default ({navigation}) => {
    const renderItem = ({item: {key, title}}) => (
        <TouchableOpacity onPress={() => navigation.navigate(key)}>
            <View style={styles.item}>
                <Text style={styles.title}>
                    {title}
                </Text>
            </View>
        </TouchableOpacity>
    );
    return (
        <FlatList data={featureData}
                  renderItem={renderItem}/>
    )
}

const styles = StyleSheet.create({
    item: {
        paddingHorizontal: 12,
        paddingVertical: 8
    },
    title: {
        fontSize: 18
    }
});