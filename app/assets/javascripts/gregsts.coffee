root = exports ? this

class gregsts
  $ = jQuery
  me = this
  source = null
  
  constructor: ->
    me = this

  displayCheckout: (cart_data) ->      
    $('#checkout').show()	
    $('#checkoutqr').empty().append('Processing')
    $('#mobile').hide()
    request = $.post '/checkout', cart_data
    request.done (data) ->
      img = $ '<img />'
      img.attr 'src', data.img_url
      urltxt = $ '<div />'
      urltxt.addClass 'alturl'
      urltxt.text data.url
      $('#checkoutqr').empty().append(img).append(urltxt)
      $('#mobile-url').val data.url 
      $('#mobile').show()
      console?.log(data.url)

    request.fail (jqXHR, status) ->
      $('#checkout').empty.append('error processing checkout')
      console?.log jqXHR
  
  listen: (url) -> 
    if !!window.EventSource
      console.log 'connecting to event source:' + url

      source?.close()
      source = new EventSource url
      msg_cb = (e) ->
        if e.data == "'done'"
          console.log e.data
          source.close()
          document.location.href = '/fulfill'
        if e.data == "'expired'"
          console.log e.data
          source.close()
          me.listen(url)
          
      open_cb = (e) ->
      	console?.log 'opened new connection'
      error_cb = (e) ->
        console?.log "error"
        console?.log e
        if e.readyState == EventSource.CLOSED
          console?.log "closed"
        else
          source.close()
      	
      source.addEventListener 'message', msg_cb, false
      source.addEventListener 'open', open_cb, false
      source.addEventListener 'error', error_cb , false

  ignore: ->
    if source? and source.readyState != EventSource.CLOSED
      console?.log 'killing connection'
      source?.close()
      source = null
  

  hideCheckout: ->
    $('#checkout').hide()

  pingMobileID: ->
     request = $.post 'https://www.securekeylabs.com/mobileid/ping', {
       'mobile_no' : $('#mobile-no').val(),
       'url' : $('#mobile-url').val()
     }
     request.done (data) ->
        console?.log 'notification sent'
        $('#mobile-progress .alturl').append(' waiting for response')
     request.fail () ->
        console?.log 'notification failed'
        $('#mobile-progress .alturl').append('failed to send notification')
        $('#mobile-progressbar').progressbar 'option', { value: 100 }
        $('#mobile-progressbar .ui-progressbar-value').css({ 'background' : '#ff4444'}); 
  	

root.gregsts = new gregsts
