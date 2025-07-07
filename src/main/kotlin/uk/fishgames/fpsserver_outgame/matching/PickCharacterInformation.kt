package uk.fishgames.fpsserver_outgame.matching

import java.time.Instant

/**
 * @see
 */
class PickCharacterInformation(public val matchStartedAt: Instant) {
    var userPickOnCharacter: HashMap<String, String> = HashMap()//User Id/Character Id | 유저가 선택하려고 올려둔 캐릭터
    var userPickUpCharacter: HashMap<String, String> = HashMap()//User Id/Character Id | 유저가 선택한 캐릭터

}