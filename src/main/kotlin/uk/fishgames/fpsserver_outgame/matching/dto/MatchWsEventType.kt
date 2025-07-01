package uk.fishgames.fpsserver_outgame.matching.dto

enum class MatchWsEventType {
    //client->server
    Ping,


    EnqueueMatch,

    PickCharacter,
    GetPickInformation,

    //server->client
    Pong,
    EnsureEnqueueMatch,
    MatchFound,
    NotifyCharacterChanged,

    Cancel,

}