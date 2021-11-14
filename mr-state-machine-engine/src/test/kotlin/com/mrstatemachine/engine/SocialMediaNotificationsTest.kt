package com.mrstatemachine.engine

class SocialMediaNotificationsTest {
    sealed class Event {
        object FacebookPostLiked : Event()
        object FacebookPostShared : Event()
        object TweetLiked : Event()
        object TweetRetweeted : Event()
    }

    sealed class State
}
