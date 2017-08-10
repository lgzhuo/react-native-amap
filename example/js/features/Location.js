/**
 * Created by lgzhuo on 2017/8/10.
 */
import React from 'react'
import {StyleSheet, View, Text, TouchableOpacity, ActivityIndicator, FlatList} from 'react-native'
import {AMapLocation} from 'react-native-amap'
import JSONTree from 'react-native-json-tree'
import fecha from 'fecha'

const Button = ({onPress, title, disabled}) => (
    <TouchableOpacity onPress={onPress} disabled={disabled}>
        <View style={styles.button}>
            <Text style={styles.buttonText}>
                {title}
            </Text>
        </View>
    </TouchableOpacity>
);

const _keyExtractor = (item, index) => index;

const Item = ({date, desc}) => (
    <View style={{paddingHorizontal: 8, paddingVertical: 4}}>
        <Text style={{fontSize: 14}}>
            {fecha.format(date, 'HH:mm:ss')}
            {' '}
            <Text style={{fontWeight: 'bold', fontSize: 15}}>
                {desc}
            </Text>
        </Text>
    </View>
);

export class Location extends React.PureComponent {

    uploadIndex = 0;
    state = {
        requestCurrentPosition: false,
        currentPosition: undefined,
        watching: false,
        locations: []
    };

    componentWillUnmount() {
        clearInterval(this._interval)
    }

    render() {
        return (
            <View style={{flex: 1}}>
                <View style={styles.requestCurrentContainer}>
                    <Button title="获取当前位置" onPress={this._getCurrentPosition}
                            disabled={this.state.requestCurrentPosition}/>
                    {this.state.requestCurrentPosition && (
                        <ActivityIndicator/>
                    )}
                    {this.state.currentPosition && (
                        <View style={{flex: 1}}>
                            <JSONTree data={this.state.currentPosition}/>
                        </View>
                    )}
                </View>
                <View style={{alignItems: 'flex-end'}}>
                    <Button title={this.state.watching ? '关闭定位监听' : '开启定位监听'} onPress={this._locationWatcherToggle}/>
                    <FlatList data={this.state.locations}
                              renderItem={this._renderItem}
                              keyExtractor={_keyExtractor}/>
                </View>
            </View>
        )
    }

    _getCurrentPosition = () => {
        this.setState({requestCurrentPosition: true, currentPosition: undefined});
        const _onReturn = data => this.setState({requestCurrentPosition: false, currentPosition: data});
        AMapLocation.getCurrentPosition(_onReturn, _onReturn, {})
    };

    _locationWatcherToggle = () => {
        this.setState(({watching}) => {
            if (watching) {
                AMapLocation.clearWatch(this._watchID);
                clearInterval(this._interval)
            } else {
                this._watchID = AMapLocation.watchPosition(this._onLocationSuccess, this._onLocationError, {
                    enableHighAccuracy: true,
                    distanceFilter: 20
                });
                this._interval = setInterval(this._uploadLocation, 60000)
            }
            return {watching: !watching}
        })
    };

    _onLocationSuccess = location => this.setState(({locations}) => ({locations: [...locations, {location}]}));

    _onLocationError = error => this.setState(({locations}) => ({
        locations: [...locations, {
            error,
            time: Date.now()
        }]
    }));

    _renderItem = ({item: {location, error, time}}) => {
        if (location) {
            return <Item date={location.timestamp}
                         desc={`lat:${location.coords.latitude} lon:${location.coords.longitude}`}/>
        } else {
            return <Item date={time} desc={`error code:${error.code} msg:${error.message}`}/>
        }
    };

    _uploadLocation = () => {
        const locations = this.state.locations,
            targetIndex = locations.length,
            startIndex = this.uploadIndex;
        if (startIndex < targetIndex) {
            const url = `http://192.168.1.107:8888/upload?data=${JSON.stringify(locations.slice(this.uploadIndex).filter(({error}) => !error).map(({
                                                                                                                                                   location: {
                                                                                                                                                       coords: {
                                                                                                                                                           latitude,
                                                                                                                                                           longitude
                                                                                                                                                       }, timestamp
                                                                                                                                                   }
                                                                                                                                               }) => ({
                latitude,
                longitude,
                time: fecha.format(timestamp, 'HH:mm:ss')
            })))}`;
            console.debug('upload location ->', url);
            fetch(url).then(() => {
                console.debug(`upload locations from ${startIndex} to ${targetIndex}`);
                this.uploadIndex = targetIndex;
            }).catch(e => {
                console.debug(`upload locations err`, e);
            })
        }
    }
}

const styles = StyleSheet.create({
    requestCurrentContainer: {
        paddingHorizontal: 12,
        flexDirection: 'row-reverse',
        alignItems: 'center'
    },
    button: {
        paddingHorizontal: 8,
        paddingVertical: 6,
        borderRadius: 4,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#888888'
    },
    buttonText: {
        fontSize: 15
    }
});