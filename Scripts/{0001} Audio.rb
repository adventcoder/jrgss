Audio.bgm_play('Audio/BGM/Airship')

weather = ['Rain', 'Wind', 'Storm']
weather_index = 0

loop do
  Graphics.update
  Input.update
  if Input.repeat?(:C)
    if Input.press?(:SHIFT)
      Audio.bgs_fade(5*1000)
    else
      Audio.bgs_play('Audio/BGS/' + weather[weather_index])
      weather_index = (weather_index + 1) % weather.size
    end
  elsif Input.repeat?(:X)
    Audio.me_play('Audio/ME/Gag')
  elsif Input.repeat?(:Y)
    Audio.me_play('Audio/ME/Victory1')
  elsif Input.repeat?(:Z)
    Audio.me_play('Audio/ME/Mystery')
  elsif Input.repeat?(:L)
    Audio.se_play('Audio/SE/Bell1')
  elsif Input.press?(:R)
    Audio.se_play('Audio/SE/Cat')
  end
  p([Audio.bgm_pos, Audio.bgs_pos])
end
