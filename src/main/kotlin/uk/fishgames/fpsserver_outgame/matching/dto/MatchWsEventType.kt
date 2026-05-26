package uk.fishgames.fpsserver_outgame.matching.dto

enum class MatchWsEventType {
    //client->server
    Ping,
    EnqueueMatch,

    PickCharacter,
    PickCharacterTemporary,
    GetGameTeamPlayerInformation,
    Cancel,

    //server->client
    JoinLobby,
    Pong,
    EnsureEnqueueMatch,
    MatchFound,
    Dodged,
    StartMatch,
    NotifyCharacterChanged,
    NotifyCharacterPicked,
    PickCharacterFailed,
    PickCharacterSuccess,
    GameInformation,
    CancelSuccess,//Cancel의 성공 응답



}